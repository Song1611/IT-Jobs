import { Skeleton } from "@/components/ui/shadcn/skeleton";
import { Card } from "@/components/ui/shadcn/card";
import { CompanyGridSkeleton } from "./company.skeleton";
import { JobGridSkeleton } from "./job.skeleton";

// ============================
// Homepage Skeleton
// ============================
export function HomeSkeleton() {
  return (
    <div>
      {/* Hero placeholder */}
      <Skeleton className="w-full h-[500px] rounded-none" />

      <div className="max-w-[1200px] mx-auto px-4 space-y-16 py-10">
        {/* Feature Cards */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          {Array.from({ length: 4 }).map((_, i) => (
            <Card key={i} className="p-6 space-y-3">
              <Skeleton className="h-10 w-10 rounded-lg" />
              <Skeleton className="h-5 w-24" />
              <Skeleton className="h-4 w-full" />
            </Card>
          ))}
        </div>

        {/* Featured Companies */}
        <div>
          <Skeleton className="h-8 w-64 mb-2" />
          <Skeleton className="h-4 w-96 mb-6" />
          <CompanyGridSkeleton count={6} />
        </div>

        {/* Job Today */}
        <div>
          <Skeleton className="h-8 w-48 mb-2" />
          <Skeleton className="h-4 w-80 mb-6" />
          <JobGridSkeleton count={4} />
        </div>
      </div>
    </div>
  );
}
