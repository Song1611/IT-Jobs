import CompanyFollow from "@/features/user/QA/right-sidebar/company-follow.section";
import BlogCompany from "@/features/user/QA/left-sidebar/QA-company.section";

export default function RightSidebar({ followList, companyPosts }) {
  return (
    <div className="h-full flex flex-col gap-4 overflow-hidden">
      {/* Chia sẻ kinh nghiệm từ công ty */}
      <div className="flex-shrink-0">
        <BlogCompany companyPosts={companyPosts} />
      </div>
      
      {/* Công ty có thể quan tâm */}
      <div className="flex-shrink-0">
        <CompanyFollow followList={followList} />
      </div>  
    </div>);

}