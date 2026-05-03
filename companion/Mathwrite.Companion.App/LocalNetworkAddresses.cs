using System.Net;
using System.Net.NetworkInformation;
using System.Net.Sockets;

namespace Mathwrite.Companion.App;

public static class LocalNetworkAddresses
{
    public static IReadOnlyList<string> GetIPv4Addresses()
    {
        return NetworkInterface.GetAllNetworkInterfaces()
            .Where(adapter =>
                adapter.OperationalStatus == OperationalStatus.Up &&
                adapter.NetworkInterfaceType != NetworkInterfaceType.Loopback)
            .SelectMany(adapter => adapter.GetIPProperties().UnicastAddresses)
            .Where(address => address.Address.AddressFamily == AddressFamily.InterNetwork)
            .Select(address => address.Address.ToString())
            .Where(address => !IPAddress.Parse(address).Equals(IPAddress.Loopback))
            .Distinct(StringComparer.Ordinal)
            .OrderBy(address => address, StringComparer.Ordinal)
            .ToArray();
    }
}
