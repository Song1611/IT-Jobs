"use client";

import { Suspense } from "react";
import SearchResultsPage from "@/features/user/search-results.page";
import { JobsPageSkeleton } from "@/components/ui/skeletons";

function SearchPageContent() {
  return <SearchResultsPage />;
}

export default function SearchPage() {
  return (
    <Suspense fallback={<JobsPageSkeleton />}>
      <SearchPageContent />
    </Suspense>);
}