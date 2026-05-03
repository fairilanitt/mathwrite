namespace Mathwrite.Companion.App;

public partial class Form1 : Form
{
    private readonly PasteHttpServer server;
    private readonly Mathwrite.Companion.Core.TabletRegistry tabletRegistry = new();
    private readonly TextBox diagnostics = new();
    private readonly ListBox tablets = new();
    private readonly Label endpointLabel = new();
    private readonly Label selectedTabletLabel = new();

    public Form1(bool fakePaste)
    {
        InitializeComponent();
        Text = "Mathwrite Companion";
        Width = 920;
        Height = 620;
        MinimumSize = new Size(760, 520);

        BuildLayout();

        Mathwrite.Companion.Core.IPasteExecutor executor = fakePaste
            ? new InMemoryPasteExecutor(AddStatus)
            : new WindowsPasteExecutor(this, AddStatus);
        var handler = new Mathwrite.Companion.Core.PasteRequestHandler(executor);
        var sketchHandler = new Mathwrite.Companion.Core.SketchPasteRequestHandler(executor);
        server = new PasteHttpServer(
            PasteHttpServer.DefaultPort,
            handler,
            sketchHandler,
            tabletRegistry,
            AddStatus,
            RefreshTablets);

        AddStatus(fakePaste ? "Starting in fake-paste mode." : "Starting with Windows clipboard paste enabled.");
        AddStatus("USB/ADB forwarding is disabled. Connect the tablet and laptop to the same network.");
    }

    protected override void OnLoad(EventArgs e)
    {
        base.OnLoad(e);
        server.Start();
        RefreshEndpointLabel();
        RefreshTablets();
    }

    protected override void OnFormClosed(FormClosedEventArgs e)
    {
        server.Dispose();
        base.OnFormClosed(e);
    }

    private void BuildLayout()
    {
        var root = new TableLayoutPanel
        {
            Dock = DockStyle.Fill,
            ColumnCount = 2,
            RowCount = 1,
            Padding = new Padding(14),
        };
        root.ColumnStyles.Add(new ColumnStyle(SizeType.Absolute, 320));
        root.ColumnStyles.Add(new ColumnStyle(SizeType.Percent, 100));
        Controls.Add(root);

        var left = new TableLayoutPanel
        {
            Dock = DockStyle.Fill,
            RowCount = 7,
            ColumnCount = 1,
        };
        left.RowStyles.Add(new RowStyle(SizeType.AutoSize));
        left.RowStyles.Add(new RowStyle(SizeType.AutoSize));
        left.RowStyles.Add(new RowStyle(SizeType.AutoSize));
        left.RowStyles.Add(new RowStyle(SizeType.Percent, 100));
        left.RowStyles.Add(new RowStyle(SizeType.AutoSize));
        left.RowStyles.Add(new RowStyle(SizeType.AutoSize));
        left.RowStyles.Add(new RowStyle(SizeType.AutoSize));
        root.Controls.Add(left, 0, 0);

        var title = new Label
        {
            AutoSize = true,
            Font = new Font(Font.FontFamily, 14, FontStyle.Bold),
            Text = "LAN Pairing",
            Margin = new Padding(0, 0, 0, 8),
        };
        left.Controls.Add(title);

        endpointLabel.AutoSize = true;
        endpointLabel.MaximumSize = new Size(290, 0);
        endpointLabel.Margin = new Padding(0, 0, 0, 10);
        left.Controls.Add(endpointLabel);

        var tabletsLabel = new Label
        {
            AutoSize = true,
            Text = "Discovered tablets",
            Font = new Font(Font, FontStyle.Bold),
            Margin = new Padding(0, 0, 0, 4),
        };
        left.Controls.Add(tabletsLabel);

        tablets.Dock = DockStyle.Fill;
        tablets.IntegralHeight = false;
        tablets.SelectedIndexChanged += (_, _) =>
        {
            if (tablets.SelectedItem is TabletListItem selected)
            {
                tabletRegistry.Select(selected.Info.SessionId);
                RefreshSelectedTabletLabel();
                AddStatus($"Selected tablet: {selected.Info.DisplayName} ({selected.Info.RemoteAddress})");
            }
        };
        left.Controls.Add(tablets);

        selectedTabletLabel.AutoSize = true;
        selectedTabletLabel.MaximumSize = new Size(290, 0);
        selectedTabletLabel.Margin = new Padding(0, 10, 0, 8);
        left.Controls.Add(selectedTabletLabel);

        var refreshButton = new Button
        {
            Text = "Refresh",
            Height = 34,
            Dock = DockStyle.Top,
            Margin = new Padding(0, 0, 0, 8),
        };
        refreshButton.Click += (_, _) =>
        {
            RefreshEndpointLabel();
            RefreshTablets();
        };
        left.Controls.Add(refreshButton);

        var note = new Label
        {
            AutoSize = true,
            MaximumSize = new Size(290, 0),
            Text = "The tablet app should use one of the laptop addresses above. Windows Firewall may ask you to allow this app on private networks.",
        };
        left.Controls.Add(note);

        var right = new TableLayoutPanel
        {
            Dock = DockStyle.Fill,
            RowCount = 2,
            ColumnCount = 1,
            Padding = new Padding(12, 0, 0, 0),
        };
        right.RowStyles.Add(new RowStyle(SizeType.AutoSize));
        right.RowStyles.Add(new RowStyle(SizeType.Percent, 100));
        root.Controls.Add(right, 1, 0);

        var logLabel = new Label
        {
            AutoSize = true,
            Text = "Activity",
            Font = new Font(Font, FontStyle.Bold),
            Margin = new Padding(0, 0, 0, 4),
        };
        right.Controls.Add(logLabel);

        diagnostics.Multiline = true;
        diagnostics.ReadOnly = true;
        diagnostics.ScrollBars = ScrollBars.Vertical;
        diagnostics.Dock = DockStyle.Fill;
        right.Controls.Add(diagnostics);
    }

    private void RefreshEndpointLabel()
    {
        var addresses = LocalNetworkAddresses.GetIPv4Addresses();
        endpointLabel.Text = addresses.Count == 0
            ? $"No active LAN IPv4 address found. Server is listening on port {PasteHttpServer.DefaultPort}."
            : "Laptop endpoints:\r\n" + string.Join("\r\n", addresses.Select(address => $"http://{address}:{PasteHttpServer.DefaultPort}"));
    }

    private void RefreshTablets()
    {
        if (InvokeRequired)
        {
            BeginInvoke(RefreshTablets);
            return;
        }

        var selectedSessionId = tabletRegistry.SelectedSessionId;
        tablets.BeginUpdate();
        tablets.Items.Clear();
        foreach (var tablet in tabletRegistry.Tablets)
        {
            var item = new TabletListItem(tablet);
            var index = tablets.Items.Add(item);
            if (tablet.SessionId == selectedSessionId)
            {
                tablets.SelectedIndex = index;
            }
        }

        tablets.EndUpdate();
        RefreshSelectedTabletLabel();
    }

    private void RefreshSelectedTabletLabel()
    {
        var selected = tabletRegistry.Tablets.FirstOrDefault(tablet => tablet.SessionId == tabletRegistry.SelectedSessionId);
        selectedTabletLabel.Text = selected is null
            ? "No tablet selected yet. The first tablet that connects will be selected automatically."
            : $"Selected: {selected.DisplayName}\r\n{selected.RemoteAddress}";
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

    private sealed class TabletListItem
    {
        public TabletListItem(Mathwrite.Companion.Core.TabletInfo info)
        {
            Info = info;
        }

        public Mathwrite.Companion.Core.TabletInfo Info { get; }

        public override string ToString() => $"{Info.DisplayName}  {Info.RemoteAddress}";
    }
}
