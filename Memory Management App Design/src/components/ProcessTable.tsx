import { Card } from "./ui/card";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "./ui/table";
import { Badge } from "./ui/badge";
import { Button } from "./ui/button";
import { X, ArrowUpDown } from "lucide-react";
import { useState } from "react";

interface Process {
  pid: number;
  name: string;
  user: string;
  memory: number;
  memoryPercent: number;
  status: "running" | "sleeping" | "stopped";
}

interface ProcessTableProps {
  processes: Process[];
}

export function ProcessTable({ processes }: ProcessTableProps) {
  const [sortBy, setSortBy] = useState<"memory" | "name">("memory");
  const [sortOrder, setSortOrder] = useState<"asc" | "desc">("desc");

  const formatBytes = (bytes: number) => {
    const mb = (bytes / (1024 * 1024)).toFixed(1);
    return `${mb} MB`;
  };

  const handleSort = (column: "memory" | "name") => {
    if (sortBy === column) {
      setSortOrder(sortOrder === "asc" ? "desc" : "asc");
    } else {
      setSortBy(column);
      setSortOrder("desc");
    }
  };

  const sortedProcesses = [...processes].sort((a, b) => {
    const multiplier = sortOrder === "asc" ? 1 : -1;
    if (sortBy === "memory") {
      return (a.memory - b.memory) * multiplier;
    } else {
      return a.name.localeCompare(b.name) * multiplier;
    }
  });

  const getStatusColor = (status: Process["status"]) => {
    switch (status) {
      case "running":
        return "bg-green-500/10 text-green-500 hover:bg-green-500/20";
      case "sleeping":
        return "bg-blue-500/10 text-blue-500 hover:bg-blue-500/20";
      case "stopped":
        return "bg-red-500/10 text-red-500 hover:bg-red-500/20";
    }
  };

  return (
    <Card className="p-6">
      <div className="flex items-center justify-between mb-4">
        <h3>Top Memory Consuming Processes</h3>
        <div className="flex items-center gap-2 text-sm text-muted-foreground">
          <span>{processes.length} processes</span>
        </div>
      </div>
      <div className="border rounded-lg">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>PID</TableHead>
              <TableHead>
                <Button
                  variant="ghost"
                  size="sm"
                  className="h-8 px-2"
                  onClick={() => handleSort("name")}
                >
                  Process Name
                  <ArrowUpDown className="ml-2 h-3 w-3" />
                </Button>
              </TableHead>
              <TableHead>User</TableHead>
              <TableHead>
                <Button
                  variant="ghost"
                  size="sm"
                  className="h-8 px-2"
                  onClick={() => handleSort("memory")}
                >
                  Memory
                  <ArrowUpDown className="ml-2 h-3 w-3" />
                </Button>
              </TableHead>
              <TableHead>%</TableHead>
              <TableHead>Status</TableHead>
              <TableHead className="text-right">Action</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {sortedProcesses.map((process) => (
              <TableRow key={process.pid}>
                <TableCell>{process.pid}</TableCell>
                <TableCell>{process.name}</TableCell>
                <TableCell className="text-muted-foreground">{process.user}</TableCell>
                <TableCell>{formatBytes(process.memory)}</TableCell>
                <TableCell>{process.memoryPercent.toFixed(1)}%</TableCell>
                <TableCell>
                  <Badge variant="outline" className={getStatusColor(process.status)}>
                    {process.status}
                  </Badge>
                </TableCell>
                <TableCell className="text-right">
                  <Button variant="ghost" size="sm" className="h-8 w-8 p-0">
                    <X className="h-4 w-4" />
                  </Button>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </div>
    </Card>
  );
}
