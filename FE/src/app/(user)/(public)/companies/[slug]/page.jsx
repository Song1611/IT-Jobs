import CompanyDetailPage from "@/features/company/company-detail.page";
import { companyApi } from "@/apis";
import { notFound } from "next/navigation";







export default async function Page({ params }) {
  const { slug } = await params;

  // Convert slug to ID (assuming slug is the company ID)
  const companyId = parseInt(slug);

  if (isNaN(companyId)) {
    notFound();
  }

  try {
    const company = await companyApi.getById(companyId);

    if (!company) {
      notFound();
    }

    return <CompanyDetailPage company={company} />;
  } catch (error) {
    notFound();
  }
}

export const dynamic = 'force-dynamic';