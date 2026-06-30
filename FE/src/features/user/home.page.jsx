import QASection from "@/features/user/home/QA.section";
import FeatureCards from "@/features/user/home/feature-cards.section";
import FeaturedCompanies from "@/features/user/home/feature-company.section";
import FeatureHr from "@/features/user/home/feature-hr.section";
import JobToday from "@/features/user/home/job-today.section";
import NewestJob from "@/features/user/home/newest-job.section";
import { HeroSection } from "@/features/user/common/hero.section";

function HomePage() {
  return (
    <div className="">
      <HeroSection height={500} />

      <div className="bg-background  w-full rounded-t-3xl border-t border-border/50 -mt-20 relative z-10 shadow-2xl shadow-black/5">
        <div className="max-w-[1200px] mx-auto px-4">
          <div className="z-10 -translate-y-20">
            <FeatureCards />
          </div>
          <div className="-mt-10">
            <FeaturedCompanies />
          </div>
          <div>
            <FeatureHr />
          </div>
          <div className="pt-20">
            <JobToday />
          </div>
          <div className="pt-20">
            <NewestJob />
          </div>
          <div className="py-20">
            <QASection />
          </div>
        </div>
      </div>
    </div>);

}

export default HomePage;