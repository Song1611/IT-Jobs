"use client";

import { AdminLayout } from "@/components/layouts/admin/admin.layout";
import ProtectedRoute from "@/components/auth/protected-route";

export default function RootLayout({
  children


}) {
  return (
    <ProtectedRoute allowedRoles={["admin"]}>
      <AdminLayout>{children}</AdminLayout>
    </ProtectedRoute>);

}