"use client";

import { useEffect, useState } from "react";
import Image from "next/image";
import SectionTitle from "@/components/ui/customs/section-title";
import { blogApi } from "@/services";


function QASection() {
  const [blogs, setBlogs] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function fetchBlogs() {
      try {
        setLoading(true);
        const response = await blogApi.getAll(1, 6);
        setBlogs(response.items || []);
      } catch (err) {
        console.error("❌ Error fetching blogs:", err);
        setBlogs([]);
      } finally {
        setLoading(false);
      }
    }

    fetchBlogs();
  }, []);

  if (loading) {
    return (
      <div className="w-full px-2 lg:mx-0">
        <SectionTitle
          title="Blog IT"
          subtitle="Kiến thức và kinh nghiệm từ cộng đồng IT"
          showViewAll
          viewAllLink="/blog" />
        
        <div className="grid grid-cols-1 md:grid-cols-12 gap-6">
          <div className="md:col-span-7">
            <div className="w-full h-[350px] rounded-xl bg-muted animate-pulse" />
            <div className="mt-3 h-6 w-3/4 bg-muted animate-pulse rounded" />
            <div className="mt-2 h-4 w-full bg-muted animate-pulse rounded" />
          </div>
          <div className="md:col-span-5 flex flex-col gap-4">
            {Array.from({ length: 4 }).map((_, i) => (
              <div key={i} className="flex gap-3 border-b pb-3">
                <div className="w-[150px] h-[80px] rounded-lg bg-muted animate-pulse flex-shrink-0" />
                <div className="flex-1 space-y-2">
                  <div className="h-4 w-full bg-muted animate-pulse rounded" />
                  <div className="h-3 w-3/4 bg-muted animate-pulse rounded" />
                  <div className="h-3 w-16 bg-muted animate-pulse rounded" />
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>);
  }

  const featured = blogs[0]; // Blog đầu tiên làm featured
  const others = blogs.slice(1); // Các blog còn lại

  return (
    <div className="w-full px-2 lg:mx-0">
      <SectionTitle
        title="Blog IT"
        subtitle="Kiến thức và kinh nghiệm từ cộng đồng IT"
        showViewAll
        viewAllLink="/blog" />
      

      <div className="grid grid-cols-1 md:grid-cols-12 gap-6">
        {featured &&
        <div className="md:col-span-7">
            <a href={`/blog/${featured.id}`} className="block group">
              <div className="w-full h-[220px] md:h-[350px] relative overflow-hidden shadow rounded-xl">
                <Image
                src={featured.image || "/cover.png"}
                alt={featured.title}
                fill
                className="object-cover group-hover:scale-105 transition"
                sizes="(max-width: 768px) 100vw, 58vw" />
              
              </div>
              <h3 className="mt-3 font-bold text-lg md:text-xl group-hover:text-primary transition">
                {featured.title}
              </h3>
              <p className="text-sm text-muted-foreground mt-1 line-clamp-2 md:line-clamp-3">
                {featured.excerpt}
              </p>
              <div className="flex items-center gap-2 mt-2 text-xs text-muted-foreground">
                <span>{featured.author?.fullName || featured.author}</span>
                <span>•</span>
                <span>{featured.readTime}</span>
                <span>•</span>
                <span>{featured.createdAt ? new Date(featured.createdAt).toLocaleDateString("vi-VN") : ""}</span>
              </div>
              <span className="text-primary text-sm mt-1 inline-block hover:underline">
                Đọc thêm
              </span>
            </a>
          </div>
        }

        <div className="md:col-span-5 flex flex-col gap-4 max-h-[450px] overflow-auto">
          {others.map((blog, index) =>
          <a
            key={blog.id || index}
            href={`/blog/${blog.id || index}`}
            className="flex gap-3 group border-b pb-3 last:border-none">
            
              <div className="w-[120px] md:w-[150px] h-[80px] relative overflow-hidden flex-shrink-0 rounded-lg">
                <Image
                src={blog.image || "/cover.png"}
                alt={blog.title}
                fill
                className="object-cover group-hover:scale-105 transition"
                sizes="150px" />
              
              </div>
              <div className="flex-1">
                <h4 className="font-semibold text-sm md:text-base group-hover:text-primary transition line-clamp-2">
                  {blog.title}
                </h4>
                <p className="text-xs md:text-sm text-muted-foreground mt-1 line-clamp-2">
                  {blog.excerpt}
                </p>
                <div className="flex items-center gap-2 mt-1 text-xs text-muted-foreground">
                  <span>{blog.readTime}</span>
                </div>
              </div>
            </a>
          )}
        </div>
      </div>
    </div>);

}

export default QASection;