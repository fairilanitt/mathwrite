using System.Net;
using System.Net.Sockets;
using System.Text;
using System.Text.Json;
using System.Text.Json.Serialization;
using Mathwrite.Companion.Core;

namespace Mathwrite.Companion.App;

public sealed class PasteHttpServer : IDisposable
{
    public const int DefaultPort = 18765;

    private static readonly JsonSerializerOptions JsonOptions = new()
    {
        PropertyNameCaseInsensitive = true,
        PropertyNamingPolicy = JsonNamingPolicy.CamelCase,
        Converters = { new JsonStringEnumConverter(JsonNamingPolicy.CamelCase) }
    };

    private readonly int port;
    private readonly PasteRequestHandler pasteHandler;
    private readonly SketchPasteRequestHandler sketchHandler;
    private readonly TabletRegistry tabletRegistry;
    private readonly Action<string> log;
    private readonly Action tabletsChanged;
    private CancellationTokenSource? cancellation;
    private TcpListener? listener;
    private Task? acceptTask;

    public PasteHttpServer(
        int port,
        PasteRequestHandler pasteHandler,
        SketchPasteRequestHandler sketchHandler,
        TabletRegistry tabletRegistry,
        Action<string> log,
        Action tabletsChanged)
    {
        this.port = port;
        this.pasteHandler = pasteHandler;
        this.sketchHandler = sketchHandler;
        this.tabletRegistry = tabletRegistry;
        this.log = log;
        this.tabletsChanged = tabletsChanged;
    }

    public void Start()
    {
        if (listener is not null)
        {
            return;
        }

        cancellation = new CancellationTokenSource();
        listener = new TcpListener(IPAddress.Any, port);
        listener.Start();
        acceptTask = Task.Run(() => AcceptLoopAsync(listener, cancellation.Token));
        log($"LAN endpoint listening on port {port}");
    }

    public void Stop()
    {
        cancellation?.Cancel();
        listener?.Stop();
        listener = null;
    }

    public void Dispose()
    {
        Stop();
        cancellation?.Dispose();
        acceptTask?.Dispose();
    }

    private async Task AcceptLoopAsync(TcpListener activeListener, CancellationToken cancellationToken)
    {
        while (!cancellationToken.IsCancellationRequested)
        {
            TcpClient client;
            try
            {
                client = await activeListener.AcceptTcpClientAsync(cancellationToken).ConfigureAwait(false);
            }
            catch (OperationCanceledException)
            {
                return;
            }
            catch (ObjectDisposedException)
            {
                return;
            }
            catch (SocketException)
            {
                return;
            }

            _ = Task.Run(() => HandleClientAsync(client, cancellationToken), cancellationToken);
        }
    }

    private async Task HandleClientAsync(TcpClient client, CancellationToken cancellationToken)
    {
        using var activeClient = client;
        try
        {
            var request = await HttpRequest.ReadAsync(client, cancellationToken).ConfigureAwait(false);
            if (request is null)
            {
                return;
            }

            var remoteAddress = client.Client.RemoteEndPoint is IPEndPoint remoteEndpoint
                ? remoteEndpoint.Address.ToString()
                : "unknown";

            if (request.Method == "GET" && request.Path == "/hello")
            {
                await WriteJsonAsync(client, HttpStatusCode.OK, CompanionHelloResponse.Create(port), cancellationToken).ConfigureAwait(false);
                return;
            }

            if (request.Method == "POST" && request.Path == "/tablet/hello")
            {
                var hello = JsonSerializer.Deserialize<TabletHelloRequest>(request.Body, JsonOptions);
                if (hello is null || !string.Equals(hello.Source, PasteRequestValidator.ExpectedSource, StringComparison.Ordinal))
                {
                    await WriteJsonAsync(client, HttpStatusCode.BadRequest, new PasteResponse(false, false, 0, "invalid_hello", "Tablet hello was invalid."), cancellationToken).ConfigureAwait(false);
                    return;
                }

                RegisterTablet(hello.SessionId, hello.TabletName, remoteAddress);
                await WriteJsonAsync(client, HttpStatusCode.OK, new PasteResponse(true, false, 0, Message: "Tablet registered."), cancellationToken).ConfigureAwait(false);
                return;
            }

            if (request.Method == "POST" && request.Path == "/paste")
            {
                var pasteRequest = JsonSerializer.Deserialize<PasteRequest>(request.Body, JsonOptions);
                if (pasteRequest is null)
                {
                    await WriteJsonAsync(client, HttpStatusCode.BadRequest, new PasteResponse(false, false, 0, "invalid_json", "Paste request was invalid."), cancellationToken).ConfigureAwait(false);
                    return;
                }

                RegisterTablet(pasteRequest.SessionId, "Mathwrite Tablet", remoteAddress);
                if (!tabletRegistry.IsAllowed(pasteRequest.SessionId))
                {
                    await WriteJsonAsync(client, HttpStatusCode.Forbidden, new PasteResponse(false, false, pasteRequest.SequenceId, "tablet_not_selected", "This tablet is not selected in Mathwrite Companion."), cancellationToken).ConfigureAwait(false);
                    log($"Rejected tablet {pasteRequest.SessionId}: not selected");
                    return;
                }

                var response = await pasteHandler.HandleAsync(pasteRequest, cancellationToken).ConfigureAwait(false);
                await WritePasteResponseAsync(client, response, cancellationToken).ConfigureAwait(false);
                log(response.Ok ? $"Pasted LaTeX request {response.SequenceId}" : $"Rejected LaTeX request {response.SequenceId}: {response.ErrorCode}");
                return;
            }

            if (request.Method == "POST" && request.Path == "/sketch")
            {
                var sketchRequest = JsonSerializer.Deserialize<SketchPasteRequest>(request.Body, JsonOptions);
                if (sketchRequest is null)
                {
                    await WriteJsonAsync(client, HttpStatusCode.BadRequest, new PasteResponse(false, false, 0, "invalid_json", "Sketch request was invalid."), cancellationToken).ConfigureAwait(false);
                    return;
                }

                RegisterTablet(sketchRequest.SessionId, sketchRequest.TabletName, remoteAddress);
                if (!tabletRegistry.IsAllowed(sketchRequest.SessionId))
                {
                    await WriteJsonAsync(client, HttpStatusCode.Forbidden, new PasteResponse(false, false, sketchRequest.SequenceId, "tablet_not_selected", "This tablet is not selected in Mathwrite Companion."), cancellationToken).ConfigureAwait(false);
                    log($"Rejected tablet {sketchRequest.SessionId}: not selected");
                    return;
                }

                var response = await sketchHandler.HandleAsync(sketchRequest, cancellationToken).ConfigureAwait(false);
                await WritePasteResponseAsync(client, response, cancellationToken).ConfigureAwait(false);
                log(response.Ok ? $"Pasted sketch request {response.SequenceId}" : $"Rejected sketch request {response.SequenceId}: {response.ErrorCode}");
                return;
            }

            await WriteJsonAsync(client, HttpStatusCode.NotFound, new PasteResponse(false, false, 0, "not_found", "Use GET /hello, POST /tablet/hello, POST /paste, or POST /sketch."), cancellationToken).ConfigureAwait(false);
        }
        catch (JsonException exception)
        {
            await WriteJsonAsync(client, HttpStatusCode.BadRequest, new PasteResponse(false, false, 0, "invalid_json", exception.Message), CancellationToken.None).ConfigureAwait(false);
        }
        catch (Exception exception)
        {
            log("LAN server error: " + exception.Message);
            if (client.Connected)
            {
                await WriteJsonAsync(client, HttpStatusCode.InternalServerError, new PasteResponse(false, false, 0, "server_error", exception.Message), CancellationToken.None).ConfigureAwait(false);
            }
        }
    }

    private void RegisterTablet(string? sessionId, string? tabletName, string remoteAddress)
    {
        var tablet = tabletRegistry.RegisterOrUpdate(sessionId, tabletName, remoteAddress);
        log($"Tablet seen: {tablet.DisplayName} at {tablet.RemoteAddress}");
        tabletsChanged();
    }

    private static Task WritePasteResponseAsync(TcpClient client, PasteResponse response, CancellationToken cancellationToken)
    {
        var statusCode = response.Ok ? HttpStatusCode.OK : HttpStatusCode.BadRequest;
        return WriteJsonAsync(client, statusCode, response, cancellationToken);
    }

    private static async Task WriteJsonAsync(TcpClient client, HttpStatusCode statusCode, object body, CancellationToken cancellationToken)
    {
        var responseBody = JsonSerializer.Serialize(body, JsonOptions);
        var responseBytes = Encoding.UTF8.GetBytes(responseBody);
        var header =
            $"HTTP/1.1 {(int)statusCode} {statusCode}\r\n" +
            "Content-Type: application/json; charset=utf-8\r\n" +
            $"Content-Length: {responseBytes.Length}\r\n" +
            "Connection: close\r\n" +
            "\r\n";
        var headerBytes = Encoding.ASCII.GetBytes(header);
        var stream = client.GetStream();
        await stream.WriteAsync(headerBytes, cancellationToken).ConfigureAwait(false);
        await stream.WriteAsync(responseBytes, cancellationToken).ConfigureAwait(false);
    }

    private sealed record TabletHelloRequest(string? SessionId, string? TabletName, string? Source);

    private sealed record CompanionHelloResponse(string Name, int Port, string[] Addresses)
    {
        public static CompanionHelloResponse Create(int port) =>
            new("Mathwrite Companion", port, LocalNetworkAddresses.GetIPv4Addresses().ToArray());
    }

    private sealed record HttpRequest(string Method, string Path, string Body)
    {
        public static async Task<HttpRequest?> ReadAsync(TcpClient client, CancellationToken cancellationToken)
        {
            var stream = client.GetStream();
            using var reader = new StreamReader(stream, Encoding.UTF8, leaveOpen: true);
            var requestLine = await reader.ReadLineAsync(cancellationToken).ConfigureAwait(false);
            if (string.IsNullOrWhiteSpace(requestLine))
            {
                return null;
            }

            var requestParts = requestLine.Split(' ', 3, StringSplitOptions.RemoveEmptyEntries);
            if (requestParts.Length < 2)
            {
                return null;
            }

            var contentLength = 0;
            string? line;
            while (!string.IsNullOrEmpty(line = await reader.ReadLineAsync(cancellationToken).ConfigureAwait(false)))
            {
                var separator = line.IndexOf(':');
                if (separator < 0)
                {
                    continue;
                }

                var name = line[..separator].Trim();
                var value = line[(separator + 1)..].Trim();
                if (string.Equals(name, "Content-Length", StringComparison.OrdinalIgnoreCase))
                {
                    _ = int.TryParse(value, out contentLength);
                }
            }

            var body = string.Empty;
            if (contentLength > 0)
            {
                var buffer = new char[contentLength];
                var read = 0;
                while (read < contentLength)
                {
                    var count = await reader.ReadAsync(buffer.AsMemory(read, contentLength - read), cancellationToken).ConfigureAwait(false);
                    if (count == 0)
                    {
                        break;
                    }

                    read += count;
                }

                body = new string(buffer, 0, read);
            }

            var path = requestParts[1].Split('?', 2)[0];
            return new HttpRequest(requestParts[0].ToUpperInvariant(), path, body);
        }
    }
}
