import { Skeleton } from "@/components/ui/shadcn/skeleton";
import { Card } from "@/components/ui/shadcn/card";

// ============================
// Company Card Skeleton
// ============================
export function CompanyCardSkeleton() {
  return (
    <Card className="p-6 flex flex-col items-center text-center">
      <Skeleton className="w-16 h-16 rounded-full mb-4" />
      <Skeleton className="h-5 w-32 mb-2" />
      <Skeleton className="h-4 w-24 mb-4" />
      <Skeleton className="h-8 w-full rounded-md" />
    </Card>
  );
}

export function CompanyGridSkeleton({ count = 6 }) {
  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
      {Array.from({ length: count }).map((_, i) => (
        <CompanyCardSkeleton key={i} />
      ))}
    </div>
  );
}

// ============================
// Company Page Skeleton
// ============================
export function CompanyPageSkeleton() {
  return (
    <div className="min-h-screen bg-background">
      <Skeleton className="w-full h-[250px] rounded-none mb-0" />
      <div className="max-w-7xl mx-auto px-4 py-8">
        <div className="mb-6 flex gap-3">
          <Skeleton className="h-9 flex-1 max-w-sm rounded-md" />
          <Skeleton className="h-9 w-32 rounded-md" />
        </div>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
          {Array.from({ length: 8 }).map((_, i) => (
            <CompanyCardSkeleton key={i} />
          ))}
        </div>
      </div>
    </div>
  );
}
