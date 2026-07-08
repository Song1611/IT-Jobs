"use client";

import { useEffect, useState } from "react";
import LogoLoop from "@/components/ui/react.bits/logo.loop";
import SectionTitle from "@/components/ui/customs/section-title";
import { companyApi } from "@/services";







function FeatureHr() {
  const [logos, setLogos] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function fetchCompanyLogos() {
      try {
        setLoading(true);
        const response = await companyApi.getLogos(1, 20);

        // Xử lý data có thể là array hoặc object với $values
        const companiesData = Array.isArray(response) ? response : (response?.items || response?.data || []);
        
        console.log("Fetched HR Companies (Logos) Data:", response, companiesData);

        // Transform API data to LogoLoop format
        const transformedLogos = companiesData.map((company) => ({
          src: company.avatar || "/logo-company.jpg",
          alt: company.name,
          href: `/companies/${company.id}`,
          title: company.name
        }));
        setLogos(transformedLogos);
      } catch (error) {
        // Fallback to empty array on error
        setLogos([]);
      } finally {
        setLoading(false);
      }
    }

    fetchCompanyLogos();
  }, []);
  if (loading) {
    return (
      <div>
        <SectionTitle title="Nhà Tuyển Dụng Hàng Đầu" />
        <div className="mt-10 flex items-center justify-center gap-8 h-24 overflow-hidden">
          {Array.from({ length: 8 }).map((_, i) => (
            <div key={i} className="w-16 h-16 rounded-lg bg-muted animate-pulse flex-shrink-0" />
          ))}
        </div>
      </div>);
  }

  if (logos.length === 0) {
    return (
      <div>
        <SectionTitle title="Nhà Tuyển Dụng Hàng Đầu" />
        <div className="mt-10 flex items-center justify-center h-24">
          <div className="text-muted-foreground">Không có dữ liệu</div>
        </div>
      </div>);

  }

  return (
    <div>
      <SectionTitle title="Nhà Tuyển Dụng Hàng Đầu" />
      <div className="mt-10">
        <LogoLoop
          logos={logos}
          speed={120}
          direction="left"
          logoHeight={60}
          gap={40}
          pauseOnHover
          scaleOnHover
          ariaLabel="Nhà tuyển dụng hàng đầu" />
        
      </div>
    </div>);

}

export default FeatureHr;