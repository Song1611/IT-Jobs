import { Skeleton } from "@/components/ui/shadcn/skeleton";
import { Card } from "@/components/ui/shadcn/card";

// ============================
// Profile / Form Skeleton
// ============================
export function ProfileSkeleton() {
  return (
    <div className="space-y-6">
      {/* Avatar + Name */}
      <Card className="p-6">
        <div className="flex items-center gap-6">
          <Skeleton className="w-20 h-20 rounded-full" />
          <div className="space-y-2 flex-1">
            <Skeleton className="h-7 w-48" />
            <Skeleton className="h-4 w-32" />
          </div>
          <Skeleton className="h-10 w-24 rounded-md" />
        </div>
      </Card>

      {/* Form fields */}
      <Card className="p-6 space-y-4">
        <Skeleton className="h-6 w-36 mb-2" />
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {Array.from({ length: 6 }).map((_, i) => (
            <div key={i} className="space-y-2">
              <Skeleton className="h-4 w-24" />
              <Skeleton className="h-10 w-full rounded-md" />
            </div>
          ))}
        </div>
      </Card>
    </div>
  );
}

// ============================
// Applied Jobs Skeleton
// ============================
export function AppliedJobsSkeleton({ count = 5 }) {
  return (
    <div className="container mx-auto px-4 py-8 max-w-5xl">
      <div className="space-y-6">
        <div className="flex justify-between items-center">
          <Skeleton className="h-9 w-52" />
          <Skeleton className="h-4 w-32" />
        </div>
        <div className="space-y-4">
          {Array.from({ length: count }).map((_, i) => (
            <Card key={i} className="p-6">
              <div className="flex items-start justify-between gap-4">
                <div className="flex gap-4 flex-1">
                  <Skeleton className="h-12 w-12 rounded-lg flex-shrink-0" />
                  <div className="flex-1 space-y-2">
                    <Skeleton className="h-5 w-3/4" />
                    <Skeleton className="h-4 w-1/3" />
                    <div className="flex gap-3">
                      <Skeleton className="h-4 w-32" />
                      <Skeleton className="h-4 w-24" />
                    </div>
                  </div>
                </div>
                <Skeleton className="h-6 w-24 rounded-full flex-shrink-0" />
              </div>
              <div className="mt-4 p-3 rounded-lg bg-muted/30">
                <Skeleton className="h-4 w-24 mb-2" />
                <Skeleton className="h-3 w-full" />
                <Skeleton className="h-3 w-4/5 mt-1" />
              </div>
              <div className="mt-4">
                <Skeleton className="h-8 w-32 rounded-md" />
              </div>
            </Card>
          ))}
        </div>
      </div>
    </div>
  );
}
