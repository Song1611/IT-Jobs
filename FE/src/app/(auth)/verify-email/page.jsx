"use client";

import { Suspense } from "react";
import VerifyEmailPage from "@/features/auth/verify-email.page";

function Page() {
  return (
    <Suspense fallback={null}>
      <VerifyEmailPage />
    </Suspense>
  );
}

export default Page;
