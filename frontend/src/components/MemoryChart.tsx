import { Card } from "./ui/card";
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from "recharts";

interface MemoryChartProps {
  data: Array<{
    time: string;
    used: number;
    cached: number;
    available: number;
  }>;
}

export function MemoryChart({ data }: MemoryChartProps) {
  const formatBytes = (bytes: number) => {
    const gb = (bytes / (1024 * 1024 * 1024)).toFixed(1);
    return `${gb} GB`;
  };

  return (
    <Card className="p-6">
      <h3 className="mb-4">Memory Usage Over Time</h3>
      <ResponsiveContainer width="100%" height={300}>
        <LineChart data={data}>
          <CartesianGrid strokeDasharray="3 3" className="stroke-muted" />
          <XAxis 
            dataKey="time" 
            className="text-xs"
            tick={{ fill: 'hsl(var(--muted-foreground))' }}
          />
          <YAxis 
            tickFormatter={formatBytes}
            className="text-xs"
            tick={{ fill: 'hsl(var(--muted-foreground))' }}
          />
          <Tooltip 
            formatter={(value: number) => formatBytes(value)}
            contentStyle={{
              backgroundColor: 'hsl(var(--card))',
              border: '1px solid hsl(var(--border))',
              borderRadius: '8px'
            }}
          />
          <Legend />
          <Line 
            type="monotone" 
            dataKey="used" 
            stroke="#f97316" 
            strokeWidth={2}
            name="Used"
            dot={false}
          />
          <Line 
            type="monotone" 
            dataKey="cached" 
            stroke="#a855f7" 
            strokeWidth={2}
            name="Cached"
            dot={false}
          />
          <Line 
            type="monotone" 
            dataKey="available" 
            stroke="#22c55e" 
            strokeWidth={2}
            name="Available"
            dot={false}
          />
        </LineChart>
      </ResponsiveContainer>
    </Card>
  );
}
