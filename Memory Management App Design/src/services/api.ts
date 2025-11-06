// API Service để gọi Java backend
const API_BASE_URL = 'http://localhost:8080/api';

export interface MemoryStats {
  total: number;
  used: number;
  free: number;
  available: number;
  cached: number;
  buffers: number;
  swap_total: number;
  swap_used: number;
}

export interface Process {
  pid: number;
  name: string;
  user: string;
  memory: number;
  memoryPercent: number;
  status: 'running' | 'sleeping' | 'stopped';
}

export interface ChartDataPoint {
  time: string;
  used: number;
  cached: number;
  available: number;
}

class ApiService {
  private chartDataHistory: ChartDataPoint[] = [];
  private maxDataPoints = 20;

  async getMemoryInfo(): Promise<MemoryStats> {
    try {
      const response = await fetch(`${API_BASE_URL}/memory`);
      if (!response.ok) throw new Error('Failed to fetch memory info');
      
      const data = await response.json();
      
      // Convert từ KB sang bytes
      return {
        total: data.totalKB * 1024,
        used: data.usedKB * 1024,
        cached: data.cachedKB * 1024,
        available: data.availableKB * 1024,
        free: (data.availableKB - data.cachedKB) * 1024,
        buffers: 0, // Có thể thêm nếu backend có
        swap_total: data.swapTotalKB * 1024,
        swap_used: data.swapUsedKB * 1024,
      };
    } catch (error) {
      console.error('Error fetching memory info:', error);
      throw error;
    }
  }

  async getProcesses(): Promise<Process[]> {
    try {
      const response = await fetch(`${API_BASE_URL}/processes?withCmdline=false`);
      if (!response.ok) throw new Error('Failed to fetch processes');
      
      const data = await response.json();
      
      // Map ProcessInfo từ Java sang Process interface
      return data.map((proc: any) => ({
        pid: proc.pid,
        name: proc.name || 'Unknown',
        user: proc.user || 'Unknown',
        memory: (proc.rssPages || 0) * 4096, // Giả sử page size = 4KB
        memoryPercent: proc.memoryPercent || 0,
        status: this.mapState(proc.state),
      }))
      .sort((a: Process, b: Process) => b.memory - a.memory)
      .slice(0, 20); // Top 20 processes
    } catch (error) {
      console.error('Error fetching processes:', error);
      throw error;
    }
  }

  async getChartData(): Promise<ChartDataPoint[]> {
    try {
      const memInfo = await this.getMemoryInfo();
      const now = new Date();
      const timeStr = now.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' });
      
      const newDataPoint: ChartDataPoint = {
        time: timeStr,
        used: memInfo.used,
        cached: memInfo.cached,
        available: memInfo.available,
      };
      
      // Add new data point and keep only last 20
      this.chartDataHistory.push(newDataPoint);
      if (this.chartDataHistory.length > this.maxDataPoints) {
        this.chartDataHistory.shift();
      }
      
      return [...this.chartDataHistory];
    } catch (error) {
      console.error('Error generating chart data:', error);
      throw error;
    }
  }

  private mapState(state: string): 'running' | 'sleeping' | 'stopped' {
    if (!state) return 'sleeping';
    const s = state.toUpperCase();
    if (s === 'R') return 'running';
    if (s === 'S' || s === 'D') return 'sleeping';
    return 'stopped';
  }

  async healthCheck(): Promise<boolean> {
    try {
      const response = await fetch(`${API_BASE_URL}/health`);
      return response.ok;
    } catch (error) {
      console.error('Health check failed:', error);
      return false;
    }
  }
}

export const apiService = new ApiService();
