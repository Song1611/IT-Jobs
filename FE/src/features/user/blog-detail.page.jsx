import BlogDetailSection from "@/features/user/blog/blog-detail.section";
import BlogRelatedSection from "@/features/user/blog/blog-related.section";
import React from "react";





function BlogDetailPage({ id }) {
  return (
    <div className="bg-background">
      <div className="max-w-[900px] mx-auto px-4 py-20">
        <BlogDetailSection id={id} />
        <div className="mt-16">
          <BlogRelatedSection currentId={id} />
        </div>
      </div>
    </div>);

}

export default BlogDetailPage;