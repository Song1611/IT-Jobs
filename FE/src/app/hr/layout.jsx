"use client";

import { HRLayout } from "@/components/layouts/hr/hr.layout";
import ProtectedRoute from "@/components/auth/protected-route";

export default function RootLayout({
  children


}) {
  return (
    <ProtectedRoute allowedRoles={["hr", "employer"]}>
      <HRLayout>{children}</HRLayout>
    </ProtectedRoute>);

}