import { Card } from "./ui/card";
import { Progress } from "./ui/progress";
import { MemoryStick, HardDrive, Cpu, Zap } from "lucide-react";

interface MemoryStats {
  total: number;
  used: number;
  free: number;
  available: number;
  cached: number;
  buffers: number;
  swap_total: number;
  swap_used: number;
}

interface MemoryOverviewProps {
  stats: MemoryStats;
}

export function MemoryOverview({ stats }: MemoryOverviewProps) {
  const memoryUsagePercent = (stats.used / stats.total) * 100;
  const swapUsagePercent = stats.swap_total > 0 ? (stats.swap_used / stats.swap_total) * 100 : 0;
  
  const formatBytes = (bytes: number) => {
    const gb = (bytes / (1024 * 1024 * 1024)).toFixed(2);
    return `${gb} GB`;
  };

  const statCards = [
    {
      icon: MemoryStick,
      label: "Total Memory",
      value: formatBytes(stats.total),
      color: "text-blue-500",
      bgColor: "bg-blue-500/10"
    },
    {
      icon: Zap,
      label: "Used Memory",
      value: formatBytes(stats.used),
      color: "text-orange-500",
      bgColor: "bg-orange-500/10"
    },
    {
      icon: HardDrive,
      label: "Available",
      value: formatBytes(stats.available),
      color: "text-green-500",
      bgColor: "bg-green-500/10"
    },
    {
      icon: Cpu,
      label: "Cached",
      value: formatBytes(stats.cached),
      color: "text-purple-500",
      bgColor: "bg-purple-500/10"
    }
  ];

  return (
    <div className="space-y-6">
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        {statCards.map((stat, index) => (
          <Card key={index} className="p-6">
            <div className="flex items-start justify-between">
              <div className="space-y-2 flex-1">
                <p className="text-sm text-muted-foreground">{stat.label}</p>
                <p className="text-2xl">{stat.value}</p>
              </div>
              <div className={`${stat.bgColor} p-3 rounded-lg`}>
                <stat.icon className={`h-6 w-6 ${stat.color}`} />
              </div>
            </div>
          </Card>
        ))}
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <Card className="p-6">
          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <h3 className="flex items-center gap-2">
                <MemoryStick className="h-5 w-5 text-blue-500" />
                RAM Usage
              </h3>
              <span className="text-sm">
                {formatBytes(stats.used)} / {formatBytes(stats.total)}
              </span>
            </div>
            <Progress value={memoryUsagePercent} className="h-3" />
            <div className="flex justify-between text-sm text-muted-foreground">
              <span>{memoryUsagePercent.toFixed(1)}% Used</span>
              <span>{(100 - memoryUsagePercent).toFixed(1)}% Free</span>
            </div>
          </div>
        </Card>

        <Card className="p-6">
          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <h3 className="flex items-center gap-2">
                <HardDrive className="h-5 w-5 text-purple-500" />
                Swap Usage
              </h3>
              <span className="text-sm">
                {formatBytes(stats.swap_used)} / {formatBytes(stats.swap_total)}
              </span>
            </div>
            <Progress value={swapUsagePercent} className="h-3" />
            <div className="flex justify-between text-sm text-muted-foreground">
              <span>{swapUsagePercent.toFixed(1)}% Used</span>
              <span>{(100 - swapUsagePercent).toFixed(1)}% Free</span>
            </div>
          </div>
        </Card>
      </div>

      <Card className="p-6">
        <h3 className="mb-4">Memory Distribution</h3>
        <div className="space-y-3">
          <div className="flex items-center justify-between">
            <span className="text-sm">Used</span>
            <span className="text-sm">{formatBytes(stats.used)}</span>
          </div>
          <Progress value={memoryUsagePercent} className="h-2" />
          
          <div className="flex items-center justify-between">
            <span className="text-sm">Cached</span>
            <span className="text-sm">{formatBytes(stats.cached)}</span>
          </div>
          <Progress value={(stats.cached / stats.total) * 100} className="h-2" />
          
          <div className="flex items-center justify-between">
            <span className="text-sm">Buffers</span>
            <span className="text-sm">{formatBytes(stats.buffers)}</span>
          </div>
          <Progress value={(stats.buffers / stats.total) * 100} className="h-2" />
          
          <div className="flex items-center justify-between">
            <span className="text-sm">Free</span>
            <span className="text-sm">{formatBytes(stats.free)}</span>
          </div>
          <Progress value={(stats.free / stats.total) * 100} className="h-2" />
        </div>
      </Card>
    </div>
  );
}
