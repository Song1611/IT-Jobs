"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import Routes from "@/constants/routes";

function Page() {
  const router = useRouter();

  useEffect(() => {
    router.replace(Routes.register);
  }, [router]);

  return null;
}

export default Page;