import { Skeleton } from "@/components/ui/shadcn/skeleton";
import { Card } from "@/components/ui/shadcn/card";

// ============================
// KPI Card Skeleton
// ============================
export function KpiCardSkeleton() {
  return (
    <Card className="p-6">
      <div className="flex items-center justify-between mb-4">
        <Skeleton className="h-10 w-10 rounded-lg" />
        <Skeleton className="h-5 w-16 rounded-full" />
      </div>
      <Skeleton className="h-8 w-24 mb-2" />
      <Skeleton className="h-4 w-32" />
    </Card>
  );
}

export function KpiGridSkeleton({ count = 4 }) {
  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
      {Array.from({ length: count }).map((_, i) => (
        <KpiCardSkeleton key={i} />
      ))}
    </div>
  );
}

// ============================
// Chart Skeleton
// ============================
export function ChartSkeleton() {
  return (
    <Card className="p-6">
      <Skeleton className="h-6 w-40 mb-6" />
      <div className="flex items-end gap-2 h-48">
        {Array.from({ length: 7 }).map((_, i) => (
          <Skeleton
            key={i}
            className="flex-1 rounded-t-md"
            style={{ height: `${30 + Math.random() * 70}%` }}
          />
        ))}
      </div>
      <div className="flex justify-between mt-2">
        {Array.from({ length: 7 }).map((_, i) => (
          <Skeleton key={i} className="h-3 w-8" />
        ))}
      </div>
    </Card>
  );
}

// ============================
// Table Skeleton
// ============================
export function TableSkeleton({ rows = 5, cols = 4 }) {
  return (
    <Card className="p-6">
      <Skeleton className="h-6 w-48 mb-6" />
      <div className="space-y-3">
        {/* Header row */}
        <div className="flex gap-4 pb-3 border-b border-border">
          {Array.from({ length: cols }).map((_, i) => (
            <Skeleton key={i} className="h-4 flex-1" />
          ))}
        </div>
        {/* Data rows */}
        {Array.from({ length: rows }).map((_, i) => (
          <div key={i} className="flex gap-4 items-center py-2">
            {Array.from({ length: cols }).map((_, j) => (
              <Skeleton key={j} className="h-4 flex-1" />
            ))}
          </div>
        ))}
      </div>
    </Card>
  );
}

// ============================
// Activity Feed Skeleton
// ============================
export function ActivityFeedSkeleton({ count = 5 }) {
  return (
    <Card className="p-6">
      <Skeleton className="h-6 w-36 mb-4" />
      <div className="space-y-4">
        {Array.from({ length: count }).map((_, i) => (
          <div key={i} className="flex items-start gap-3">
            <Skeleton className="w-8 h-8 rounded-full flex-shrink-0" />
            <div className="flex-1 space-y-1">
              <Skeleton className="h-4 w-full" />
              <Skeleton className="h-3 w-20" />
            </div>
          </div>
        ))}
      </div>
    </Card>
  );
}

// ============================
// Dashboard Skeleton (Admin/HR)
// ============================
export function DashboardSkeleton() {
  return (
    <div className="space-y-6 animate-in fade-in duration-300">
      {/* Header */}
      <div className="space-y-2">
        <Skeleton className="h-10 w-64" />
        <Skeleton className="h-5 w-96" />
      </div>

      {/* KPI */}
      <KpiGridSkeleton count={4} />

      {/* Chart + Activity */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2">
          <ChartSkeleton />
        </div>
        <div className="lg:col-span-1">
          <ActivityFeedSkeleton />
        </div>
      </div>

      {/* Two columns */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <TableSkeleton rows={4} cols={3} />
        <TableSkeleton rows={3} cols={3} />
      </div>
    </div>
  );
}

// ============================
// Dashboard Overview Skeleton (User)
// ============================
export function DashboardOverviewSkeleton() {
  return (
    <div className="space-y-6">
      {/* Welcome Header */}
      <div>
        <Skeleton className="h-9 w-72 mb-2" />
        <Skeleton className="h-4 w-80" />
      </div>

      {/* Stats Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        {Array.from({ length: 3 }).map((_, i) => (
          <Card key={i} className="p-6">
            <div className="flex items-center justify-between">
              <div className="space-y-2">
                <Skeleton className="h-4 w-28" />
                <Skeleton className="h-8 w-12" />
              </div>
              <Skeleton className="h-12 w-12 rounded-lg" />
            </div>
          </Card>
        ))}
      </div>

      {/* Two columns */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <Card className="p-6">
          <Skeleton className="h-6 w-36 mb-4" />
          <div className="space-y-4">
            {Array.from({ length: 3 }).map((_, i) => (
              <div key={i} className="flex items-start gap-3 pb-4 border-b last:border-0">
                <Skeleton className="w-2 h-2 rounded-full mt-2 flex-shrink-0" />
                <div className="flex-1 space-y-1">
                  <Skeleton className="h-4 w-full" />
                  <Skeleton className="h-3 w-2/3" />
                  <Skeleton className="h-3 w-16" />
                </div>
              </div>
            ))}
          </div>
        </Card>
        <Card className="p-6">
          <Skeleton className="h-6 w-28 mb-4" />
          <div className="space-y-3">
            {Array.from({ length: 3 }).map((_, i) => (
              <Skeleton key={i} className="h-10 w-full rounded-md" />
            ))}
          </div>
        </Card>
      </div>

      {/* Profile Completion */}
      <Card className="p-6">
        <Skeleton className="h-6 w-40 mb-4" />
        <Skeleton className="h-2 w-full rounded-full mb-4" />
        <div className="flex gap-2 flex-wrap">
          {Array.from({ length: 3 }).map((_, i) => (
            <Skeleton key={i} className="h-8 w-28 rounded-md" />
          ))}
        </div>
      </Card>
    </div>
  );
}

// ============================
// Management Table Skeleton (HR/Admin pages)
// ============================
export function ManagementTableSkeleton() {
  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div className="space-y-2">
          <Skeleton className="h-9 w-56" />
          <Skeleton className="h-4 w-40" />
        </div>
        <Skeleton className="h-10 w-40 rounded-md" />
      </div>
      <Card className="p-4">
        {/* Search + Filter row */}
        <div className="flex gap-3 mb-4">
          <Skeleton className="h-9 flex-1 rounded-md" />
          <Skeleton className="h-9 w-32 rounded-md" />
        </div>
        {/* Table header */}
        <div className="flex gap-4 pb-3 border-b border-border">
          {Array.from({ length: 6 }).map((_, i) => (
            <Skeleton key={i} className="h-4 flex-1" />
          ))}
        </div>
        {/* Table rows */}
        {Array.from({ length: 8 }).map((_, i) => (
          <div key={i} className="flex gap-4 items-center py-3 border-b border-border/50">
            {Array.from({ length: 6 }).map((_, j) => (
              <Skeleton key={j} className={`h-4 flex-1 ${j === 0 ? "w-2/5" : ""}`} />
            ))}
          </div>
        ))}
      </Card>
    </div>
  );
}
