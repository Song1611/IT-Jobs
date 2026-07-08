import { Skeleton } from "@/components/ui/shadcn/skeleton";
import { Card } from "@/components/ui/shadcn/card";

// ============================
// Blog Card Skeleton
// ============================
export function BlogCardSkeleton() {
  return (
    <Card className="overflow-hidden">
      <Skeleton className="w-full h-48" />
      <div className="p-4 space-y-3">
        <Skeleton className="h-5 w-3/4" />
        <Skeleton className="h-4 w-full" />
        <Skeleton className="h-4 w-2/3" />
        <div className="flex items-center gap-2 pt-2">
          <Skeleton className="w-6 h-6 rounded-full" />
          <Skeleton className="h-3 w-20" />
          <Skeleton className="h-3 w-16 ml-auto" />
        </div>
      </div>
    </Card>
  );
}

export function BlogGridSkeleton({ count = 6 }) {
  return (
    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
      {Array.from({ length: count }).map((_, i) => (
        <BlogCardSkeleton key={i} />
      ))}
    </div>
  );
}

// ============================
// Blog Detail Skeleton
// ============================
export function BlogDetailSkeleton() {
  return (
    <div className="container mx-auto px-4 py-8 max-w-4xl">
      <Skeleton className="w-full h-[350px] rounded-xl mb-8" />
      <div className="space-y-4 mb-6">
        <Skeleton className="h-8 w-3/4" />
        <div className="flex gap-4">
          <Skeleton className="h-4 w-24" />
          <Skeleton className="h-4 w-20" />
          <Skeleton className="h-4 w-16" />
        </div>
      </div>
      <div className="space-y-3">
        {Array.from({ length: 10 }).map((_, i) => (
          <Skeleton key={i} className={`h-4 ${i % 3 === 2 ? "w-3/4" : "w-full"}`} />
        ))}
      </div>
    </div>
  );
}
