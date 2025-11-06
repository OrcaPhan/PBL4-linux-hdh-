import { useState, useEffect } from "react";
import { MemoryOverview } from "./components/MemoryOverview";
import { MemoryChart } from "./components/MemoryChart";
import { ProcessTable } from "./components/ProcessTable";
import { Button } from "./components/ui/button";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "./components/ui/tabs";
import { RefreshCw, Terminal, Settings, AlertCircle } from "lucide-react";
import { apiService, type MemoryStats, type Process, type ChartDataPoint } from "./services/api";
import { Alert, AlertDescription } from "./components/ui/alert";

export default function App() {
  const [memoryStats, setMemoryStats] = useState<MemoryStats | null>(null);
  const [chartData, setChartData] = useState<ChartDataPoint[]>([]);
  const [processes, setProcesses] = useState<Process[]>([]);
  const [lastUpdate, setLastUpdate] = useState(new Date());
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isConnected, setIsConnected] = useState(false);

  const refreshData = async () => {
    try {
      setError(null);
      
      // Fetch memory info
      const memInfo = await apiService.getMemoryInfo();
      setMemoryStats(memInfo);
      
      // Fetch processes
      const procs = await apiService.getProcesses();
      setProcesses(procs);
      
      // Update chart data
      const chart = await apiService.getChartData();
      setChartData(chart);
      
      setLastUpdate(new Date());
      setIsConnected(true);
    } catch (err) {
      console.error('Error refreshing data:', err);
      setError('Failed to connect to backend. Make sure the Java server is running on port 8080.');
      setIsConnected(false);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    // Initial load
    refreshData();

    // Health check
    apiService.healthCheck().then(setIsConnected);

    // Auto refresh every 5 seconds
    const interval = setInterval(() => {
      refreshData();
    }, 5000);

    return () => clearInterval(interval);
  }, []);

  // Default stats for initial render
  const defaultStats: MemoryStats = {
    total: 0,
    used: 0,
    free: 0,
    available: 0,
    cached: 0,
    buffers: 0,
    swap_total: 0,
    swap_used: 0,
  };

  return (
    <div className="min-h-screen bg-background">
      <header className="border-b bg-card">
        <div className="container mx-auto px-6 py-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className="bg-blue-500 p-2 rounded-lg">
                <Terminal className="h-6 w-6 text-white" />
              </div>
              <div>
                <h1>Memory Manager</h1>
                <p className="text-sm text-muted-foreground">
                  Linux System Monitor {isConnected && <span className="text-green-500">● Connected</span>}
                  {!isConnected && <span className="text-red-500">● Disconnected</span>}
                </p>
              </div>
            </div>
            <div className="flex items-center gap-3">
              <div className="text-right mr-4">
                <p className="text-sm text-muted-foreground">Last updated</p>
                <p className="text-sm">{lastUpdate.toLocaleTimeString()}</p>
              </div>
              <Button variant="outline" size="icon" onClick={refreshData} disabled={isLoading}>
                <RefreshCw className={`h-4 w-4 ${isLoading ? 'animate-spin' : ''}`} />
              </Button>
              <Button variant="outline" size="icon">
                <Settings className="h-4 w-4" />
              </Button>
            </div>
          </div>
        </div>
      </header>

      <main className="container mx-auto px-6 py-8">
        {error && (
          <Alert variant="destructive" className="mb-6">
            <AlertCircle className="h-4 w-4" />
            <AlertDescription>{error}</AlertDescription>
          </Alert>
        )}

        <Tabs defaultValue="overview" className="space-y-6">
          <TabsList>
            <TabsTrigger value="overview">Overview</TabsTrigger>
            <TabsTrigger value="processes">Processes</TabsTrigger>
            <TabsTrigger value="history">History</TabsTrigger>
          </TabsList>

          <TabsContent value="overview" className="space-y-6">
            <MemoryOverview stats={memoryStats || defaultStats} />
            <MemoryChart data={chartData} />
          </TabsContent>

          <TabsContent value="processes" className="space-y-6">
            <ProcessTable processes={processes} />
          </TabsContent>

          <TabsContent value="history" className="space-y-6">
            <MemoryChart data={chartData} />
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <div className="border rounded-lg p-4 bg-card">
                <p className="text-sm text-muted-foreground mb-2">Peak Memory Usage</p>
                <p className="text-2xl">
                  {memoryStats ? `${(memoryStats.used / (1024 ** 3)).toFixed(1)} GB` : 'N/A'}
                </p>
                <p className="text-xs text-muted-foreground mt-1">Current session</p>
              </div>
              <div className="border rounded-lg p-4 bg-card">
                <p className="text-sm text-muted-foreground mb-2">Total Memory</p>
                <p className="text-2xl">
                  {memoryStats ? `${(memoryStats.total / (1024 ** 3)).toFixed(1)} GB` : 'N/A'}
                </p>
                <p className="text-xs text-muted-foreground mt-1">System capacity</p>
              </div>
              <div className="border rounded-lg p-4 bg-card">
                <p className="text-sm text-muted-foreground mb-2">Swap Usage</p>
                <p className="text-2xl">
                  {memoryStats ? `${(memoryStats.swap_used / (1024 ** 3)).toFixed(1)} GB` : 'N/A'}
                </p>
                <p className="text-xs text-muted-foreground mt-1">Current</p>
              </div>
            </div>
          </TabsContent>
        </Tabs>
      </main>
    </div>
  );
}
