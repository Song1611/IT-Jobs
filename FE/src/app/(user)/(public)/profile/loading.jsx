import { ProfileSkeleton } from "@/components/ui/skeletons";

export default function Loading() {
  return (
    <div className="max-w-4xl mx-auto px-4 py-8">
      <ProfileSkeleton />
    </div>
  );
}
