import ProfilePage from "@/features/user/profile.page";

export default async function Page({ params }) {
  const { userId } = await params;
  return <ProfilePage userId={userId} />;
}