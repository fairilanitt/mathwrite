using System.Net;
using System.Text;
using System.Text.Json;
using System.Text.Json.Serialization;
using Mathwrite.Companion.Core;

namespace Mathwrite.Companion.App;

public sealed class PasteHttpServer : IDisposable
{
    private static readonly JsonSerializerOptions JsonOptions = new()
    {
        PropertyNameCaseInsensitive = true,
        PropertyNamingPolicy = JsonNamingPolicy.CamelCase,
        Converters = { new JsonStringEnumConverter(JsonNamingPolicy.CamelCase) }
    };

    private readonly HttpListener listener = new();
    private readonly PasteRequestHandler handler;
    private readonly Action<string> log;
    private CancellationTokenSource? cancellation;
    private Task? acceptTask;

    public PasteHttpServer(string prefix, PasteRequestHandler handler, Action<string> log)
    {
        this.handler = handler;
        this.log = log;
        listener.Prefixes.Add(prefix);
    }

    public void Start()
    {
        if (listener.IsListening)
        {
            return;
        }

        cancellation = new CancellationTokenSource();
        listener.Start();
        acceptTask = Task.Run(() => AcceptLoopAsync(cancellation.Token));
        log("Paste endpoint listening on http://127.0.0.1:18765/paste");
    }

    public void Stop()
    {
        cancellation?.Cancel();

        if (listener.IsListening)
        {
            listener.Stop();
        }
    }

    public void Dispose()
    {
        Stop();
        cancellation?.Dispose();
        listener.Close();
        acceptTask?.Dispose();
    }

    private async Task AcceptLoopAsync(CancellationToken cancellationToken)
    {
        while (!cancellationToken.IsCancellationRequested)
        {
            HttpListenerContext context;
            try
            {
                context = await listener.GetContextAsync().ConfigureAwait(false);
            }
            catch (ObjectDisposedException)
            {
                return;
            }
            catch (HttpListenerException)
            {
                return;
            }

            _ = Task.Run(() => HandleContextAsync(context, cancellationToken), cancellationToken);
        }
    }

    private async Task HandleContextAsync(HttpListenerContext context, CancellationToken cancellationToken)
    {
        try
        {
            if (!string.Equals(context.Request.HttpMethod, "POST", StringComparison.OrdinalIgnoreCase) ||
                !string.Equals(context.Request.Url?.AbsolutePath, "/paste", StringComparison.OrdinalIgnoreCase))
            {
                context.Response.StatusCode = (int)HttpStatusCode.NotFound;
                await WriteJsonAsync(context.Response, new PasteResponse(false, false, 0, "not_found", "Use POST /paste."), cancellationToken).ConfigureAwait(false);
                return;
            }

            using var reader = new StreamReader(context.Request.InputStream, context.Request.ContentEncoding);
            var body = await reader.ReadToEndAsync(cancellationToken).ConfigureAwait(false);
            var request = JsonSerializer.Deserialize<PasteRequest>(body, JsonOptions);

            if (request is null)
            {
                context.Response.StatusCode = (int)HttpStatusCode.BadRequest;
                await WriteJsonAsync(context.Response, new PasteResponse(false, false, 0, "invalid_json", "Request JSON was empty or invalid."), cancellationToken).ConfigureAwait(false);
                return;
            }

            var response = await handler.HandleAsync(request, cancellationToken).ConfigureAwait(false);
            context.Response.StatusCode = response.Ok ? (int)HttpStatusCode.OK : (int)HttpStatusCode.BadRequest;
            await WriteJsonAsync(context.Response, response, cancellationToken).ConfigureAwait(false);
            log(response.Ok ? $"Pasted request {response.SequenceId}" : $"Rejected request {response.SequenceId}: {response.ErrorCode}");
        }
        catch (JsonException exception)
        {
            context.Response.StatusCode = (int)HttpStatusCode.BadRequest;
            await WriteJsonAsync(context.Response, new PasteResponse(false, false, 0, "invalid_json", exception.Message), CancellationToken.None).ConfigureAwait(false);
        }
        catch (Exception exception)
        {
            context.Response.StatusCode = (int)HttpStatusCode.InternalServerError;
            await WriteJsonAsync(context.Response, new PasteResponse(false, false, 0, "server_error", exception.Message), CancellationToken.None).ConfigureAwait(false);
            log("Server error: " + exception.Message);
        }
    }

    private static async Task WriteJsonAsync(HttpListenerResponse response, PasteResponse body, CancellationToken cancellationToken)
    {
        var bytes = Encoding.UTF8.GetBytes(JsonSerializer.Serialize(body, JsonOptions));
        response.ContentType = "application/json";
        response.ContentEncoding = Encoding.UTF8;
        response.ContentLength64 = bytes.Length;
        await response.OutputStream.WriteAsync(bytes, cancellationToken).ConfigureAwait(false);
        response.Close();
    }
}
