"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { isAuthenticated } from "@/lib/auth";

export default function AuthGuard({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const [checked, setChecked] = useState(false);
  const [allowed, setAllowed] = useState(false);

  useEffect(() => {
    const authed = isAuthenticated();
    setAllowed(authed);
    setChecked(true);
    if (!authed) {
      router.replace("/login");
    }
  }, [router]);

  if (!checked || !allowed) {
    return (
      <div className="flex h-full items-center justify-center text-[13px] text-[var(--text-muted)]">
        Carregando...
      </div>
    );
  }

  return <>{children}</>;
}
