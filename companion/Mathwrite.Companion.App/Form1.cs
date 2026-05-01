namespace Mathwrite.Companion.App;

public partial class Form1 : Form
{
    private readonly PasteHttpServer server;
    private readonly AdbBridgeService bridge;
    private readonly TextBox diagnostics = new();

    public Form1(bool fakePaste)
    {
        InitializeComponent();
        Text = "Mathwrite Companion";
        Width = 760;
        Height = 420;

        diagnostics.Multiline = true;
        diagnostics.ReadOnly = true;
        diagnostics.ScrollBars = ScrollBars.Vertical;
        diagnostics.Dock = DockStyle.Fill;
        Controls.Add(diagnostics);

        Mathwrite.Companion.Core.IPasteExecutor executor = fakePaste
            ? new InMemoryPasteExecutor(AddStatus)
            : new WindowsPasteExecutor(this, AddStatus);
        var handler = new Mathwrite.Companion.Core.PasteRequestHandler(executor);
        server = new PasteHttpServer("http://127.0.0.1:18765/", handler, AddStatus);
        bridge = new AdbBridgeService(AddStatus);

        AddStatus(fakePaste ? "Starting in fake-paste mode." : "Starting with Windows clipboard paste enabled.");
    }

    protected override void OnLoad(EventArgs e)
    {
        base.OnLoad(e);
        server.Start();
        _ = bridge.ApplyReverseAsync(CancellationToken.None);
    }

    protected override void OnFormClosed(FormClosedEventArgs e)
    {
        server.Dispose();
        base.OnFormClosed(e);
    }

    private void AddStatus(string message)
    {
        if (InvokeRequired)
        {
            BeginInvoke(() => AddStatus(message));
            return;
        }

        diagnostics.AppendText($"[{DateTime.Now:HH:mm:ss}] {message}{Environment.NewLine}");
    }
}
