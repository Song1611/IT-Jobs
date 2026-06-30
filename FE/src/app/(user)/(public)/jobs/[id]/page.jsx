import JobDetailPage from "@/features/user/job-detail.page";







export default async function Page({ params }) {
  const { id } = await params;
  return <JobDetailPage jobId={id} />;
}