import { CompanyGridSkeleton } from "@/components/ui/skeletons";
import { Skeleton } from "@/components/ui/shadcn/skeleton";

export default function Loading() {
  return (
    <div className="min-h-screen bg-background">
      <div className="max-w-7xl mx-auto px-4 lg:px-8 py-12">
        <div className="mb-8">
          <Skeleton className="h-8 w-48 mb-2" />
          <Skeleton className="h-4 w-80" />
        </div>
        <CompanyGridSkeleton count={6} />
      </div>
    </div>
  );
}
