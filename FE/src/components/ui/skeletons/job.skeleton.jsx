import { Skeleton } from "@/components/ui/shadcn/skeleton";
import { Card } from "@/components/ui/shadcn/card";

// ============================
// Job Card Skeleton (Grid view)
// ============================
export function JobCardSkeleton() {
  return (
    <Card className="p-5 flex flex-col items-center">
      <div className="w-full flex justify-end mb-2">
        <Skeleton className="h-5 w-12 rounded-full" />
      </div>
      <Skeleton className="w-20 h-20 rounded-lg mb-4" />
      <Skeleton className="h-5 w-3/4 mb-2" />
      <Skeleton className="h-4 w-1/2 mb-4" />
      <div className="w-full pt-4 border-t border-border">
        <div className="flex gap-1.5 justify-center">
          <Skeleton className="h-6 w-16 rounded-md" />
          <Skeleton className="h-6 w-16 rounded-md" />
          <Skeleton className="h-6 w-16 rounded-md" />
        </div>
      </div>
    </Card>
  );
}

export function JobGridSkeleton({ count = 4 }) {
  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
      {Array.from({ length: count }).map((_, i) => (
        <JobCardSkeleton key={i} />
      ))}
    </div>
  );
}

// ============================
// Job List Row Skeleton
// ============================
export function JobRowSkeleton() {
  return (
    <Card className="p-4 flex items-center gap-4">
      <Skeleton className="w-14 h-14 rounded-lg flex-shrink-0" />
      <div className="flex-1 space-y-2">
        <Skeleton className="h-5 w-2/3" />
        <Skeleton className="h-4 w-1/3" />
        <div className="flex gap-2">
          <Skeleton className="h-5 w-16 rounded-full" />
          <Skeleton className="h-5 w-20 rounded-full" />
          <Skeleton className="h-5 w-14 rounded-full" />
        </div>
      </div>
      <Skeleton className="h-9 w-24 rounded-md flex-shrink-0" />
    </Card>
  );
}

export function JobListSkeleton({ count = 6 }) {
  return (
    <div className="space-y-3">
      {Array.from({ length: count }).map((_, i) => (
        <JobRowSkeleton key={i} />
      ))}
    </div>
  );
}

// ============================
// Job Detail Skeleton
// ============================
export function JobDetailSkeleton() {
  return (
    <div className="min-h-screen bg-background">
      <div className="container mx-auto px-4 py-8 max-w-7xl">
        {/* Job Header */}
        <Card className="p-6 mb-8">
          <div className="flex flex-col md:flex-row gap-6">
            <Skeleton className="w-24 h-24 rounded-xl flex-shrink-0" />
            <div className="flex-1 space-y-3">
              <Skeleton className="h-8 w-2/3" />
              <Skeleton className="h-5 w-1/3" />
              <div className="flex gap-3 flex-wrap">
                <Skeleton className="h-7 w-24 rounded-full" />
                <Skeleton className="h-7 w-20 rounded-full" />
                <Skeleton className="h-7 w-28 rounded-full" />
              </div>
            </div>
            <Skeleton className="h-12 w-36 rounded-lg flex-shrink-0" />
          </div>
        </Card>

        {/* Content Grid */}
        <div className="grid lg:grid-cols-3 gap-8">
          <div className="lg:col-span-2 space-y-6">
            <Skeleton className="h-10 w-full rounded-lg" />
            <Card className="p-6 space-y-4">
              <Skeleton className="h-6 w-48" />
              <Skeleton className="h-4 w-full" />
              <Skeleton className="h-4 w-full" />
              <Skeleton className="h-4 w-3/4" />
              <Skeleton className="h-4 w-full" />
              <Skeleton className="h-4 w-5/6" />
            </Card>
          </div>
          <div className="lg:col-span-1">
            <Card className="p-6 space-y-4">
              <Skeleton className="w-16 h-16 rounded-full mx-auto" />
              <Skeleton className="h-5 w-32 mx-auto" />
              <Skeleton className="h-4 w-24 mx-auto" />
              <Skeleton className="h-4 w-full" />
              <Skeleton className="h-4 w-full" />
              <Skeleton className="h-10 w-full rounded-md mt-4" />
            </Card>
          </div>
        </div>
      </div>
    </div>
  );
}

// ============================
// Filter Sidebar Skeleton (Jobs page)
// ============================
export function FilterSidebarSkeleton() {
  return (
    <Card className="p-6 space-y-6">
      <Skeleton className="h-10 w-full rounded-md" />
      <div className="space-y-3">
        <Skeleton className="h-5 w-24" />
        {Array.from({ length: 5 }).map((_, i) => (
          <div key={i} className="flex items-center gap-2">
            <Skeleton className="w-4 h-4 rounded" />
            <Skeleton className="h-4 w-28" />
          </div>
        ))}
      </div>
      <div className="space-y-3">
        <Skeleton className="h-5 w-20" />
        {Array.from({ length: 4 }).map((_, i) => (
          <div key={i} className="flex items-center gap-2">
            <Skeleton className="w-4 h-4 rounded" />
            <Skeleton className="h-4 w-24" />
          </div>
        ))}
      </div>
    </Card>
  );
}

// ============================
// Jobs Page Full Skeleton
// ============================
export function JobsPageSkeleton() {
  return (
    <div className="min-h-screen bg-background">
      <Skeleton className="w-full h-[300px] rounded-none" />
      <div className="max-w-7xl mx-auto px-4 lg:px-8 py-12">
        <div className="mb-8">
          <Skeleton className="h-8 w-64 mb-2" />
          <Skeleton className="h-4 w-96" />
        </div>
        <div className="flex flex-col lg:flex-row gap-6">
          <aside className="lg:w-80 flex-shrink-0">
            <FilterSidebarSkeleton />
          </aside>
          <main className="flex-1 min-w-0">
            <JobListSkeleton count={6} />
          </main>
        </div>
      </div>
    </div>
  );
}
