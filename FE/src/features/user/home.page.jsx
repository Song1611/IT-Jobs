"use client";
import dynamic from "next/dynamic";
import { Suspense } from "react";

const HeroSection = dynamic(
  () => import("@/features/user/common/hero.section").then((m) => m.HeroSection),
  { ssr: false, loading: () => <div className="h-[500px] bg-[#1a334a]" /> }
);

const FeatureCards = dynamic(
  () => import("@/features/user/home/feature-cards.section"),
  { ssr: false }
);

const FeaturedCompanies = dynamic(
  () => import("@/features/user/home/feature-company.section"),
  { ssr: false }
);

const FeatureHr = dynamic(
  () => import("@/features/user/home/feature-hr.section"),
  { ssr: false }
);

const JobToday = dynamic(
  () => import("@/features/user/home/job-today.section"),
  { ssr: false }
);

const NewestJob = dynamic(
  () => import("@/features/user/home/newest-job.section"),
  { ssr: false }
);

const QASection = dynamic(
  () => import("@/features/user/home/QA.section"),
  { ssr: false }
);

function HomePage() {
  return (
    <div className="">
      <HeroSection height={500} />

      <div className="bg-background  w-full rounded-t-3xl border-t border-border/50 -mt-20 relative z-10 shadow-2xl shadow-black/5">
        <div className="max-w-[1200px] mx-auto px-4">
          <div className="z-10 -translate-y-20">
            <Suspense fallback={<div className="h-64 animate-pulse bg-muted/30 rounded-2xl" />}>
              <FeatureCards />
            </Suspense>
          </div>
          <div className="-mt-10">
            <Suspense fallback={null}>
              <FeaturedCompanies />
            </Suspense>
          </div>
          <div>
            <Suspense fallback={null}>
              <FeatureHr />
            </Suspense>
          </div>
          <div className="pt-20">
            <Suspense fallback={null}>
              <JobToday />
            </Suspense>
          </div>
          <div className="pt-20">
            <Suspense fallback={null}>
              <NewestJob />
            </Suspense>
          </div>
          <div className="py-20">
            <Suspense fallback={null}>
              <QASection />
            </Suspense>
          </div>
        </div>
      </div>
    </div>);

}

export default HomePage;