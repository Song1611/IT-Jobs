import BlogDetailPage from "@/features/user/blog-detail.page";

async function page({ params }) {
  const { id } = await params;
  return (
    <>
      <BlogDetailPage id={id} />
    </>);

}

export default page;