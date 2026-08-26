"use client";

import { useState, useEffect } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { authApi } from "@/services/auth.api";
import Routes from "@/constants/routes";
import { Button } from "@/components/ui/shadcn/button";
import {
  Card,
  CardHeader,
  CardTitle,
  CardContent } from "@/components/ui/shadcn/card";
import { Input } from "@/components/ui/shadcn/input";
import { Label } from "@/components/ui/shadcn/label";
import { MailCheck, MailWarning, Loader2 } from "lucide-react";
import { OTPInput } from "@/components/ui/customs/otp-input";

export default function VerifyEmailPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const emailFromQuery = searchParams.get("email") || "";

  const [email, setEmail] = useState(emailFromQuery);
  const [otp, setOtp] = useState("");
  const [error, setError] = useState("");
  const [success, setSuccess] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [isResending, setIsResending] = useState(false);
  const [countdown, setCountdown] = useState(60);

  useEffect(() => {
    if (emailFromQuery) {
      setEmail(emailFromQuery);
    }
  }, [emailFromQuery]);

  useEffect(() => {
    if (countdown <= 0) return;
    const timer = setTimeout(() => setCountdown((c) => c - 1), 1000);
    return () => clearTimeout(timer);
  }, [countdown]);

  const handleVerify = async (e) => {
    e.preventDefault();
    setError("");
    setIsLoading(true);

    try {
      await authApi.verifyEmail({ email, otp });
      setSuccess(true);
    } catch (err) {
      setError(
        err?.message || "Mã OTP không đúng hoặc đã hết hạn. Vui lòng thử lại."
      );
    } finally {
      setIsLoading(false);
    }
  };

  const handleResend = async () => {
    setError("");
    setIsResending(true);

    try {
      await authApi.resendOtp({ email });
      setCountdown(60);
    } catch (err) {
      setError(err?.message || "Không thể gửi lại mã OTP. Vui lòng thử lại.");
    } finally {
      setIsResending(false);
    }
  };

  if (success) {
    return (
      <div className="relative z-10 container mx-auto h-screen flex items-center justify-center px-4">
        <Card className="w-full max-w-md p-6 bg-white/70 dark:bg-gray-900/70 backdrop-blur-lg border border-gray-200 dark:border-gray-700 shadow-xl rounded-2xl">
          <CardHeader className="text-center">
            <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-green-500/10">
              <MailCheck className="h-8 w-8 text-green-500" />
            </div>
            <CardTitle className="text-2xl font-bold text-gray-900 dark:text-gray-100 mt-4">
              Xác thực thành công
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-4 text-center">
            <p className="text-sm text-gray-600 dark:text-gray-400">
              Email của bạn đã được xác thực. Giờ bạn có thể đăng nhập để bắt đầu.
            </p>
            <Button
              onClick={() => router.push(Routes.login)}
              className="w-full bg-gradient-to-r from-green-600 to-teal-600 hover:from-green-500 hover:to-teal-500 text-white font-semibold">
              
              Đăng nhập ngay
            </Button>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="relative z-10 container mx-auto h-screen flex items-center justify-center px-4">
      <Card className="w-full max-w-md p-6 bg-white/70 dark:bg-gray-900/70 backdrop-blur-lg border border-gray-200 dark:border-gray-700 shadow-xl rounded-2xl">
        <CardHeader className="text-center">
          <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-blue-500/10">
            <MailWarning className="h-8 w-8 text-blue-500" />
          </div>
          <CardTitle className="text-2xl font-bold text-gray-900 dark:text-gray-100 mt-4">
            Xác thực email
          </CardTitle>
        </CardHeader>
        <CardContent>
          <p className="text-sm text-center text-gray-600 dark:text-gray-400 mb-8">
            Chúng tôi đã gửi mã gồm 6 chữ số tới{" "}
            <span className="font-semibold text-gray-900 dark:text-gray-100">
              {email}
            </span>
            . Vui lòng nhập mã đó dưới đây để tiếp tục.
          </p>

          <form onSubmit={handleVerify} className="space-y-6">

            <div className="space-y-4">
              <Label className="text-sm font-medium text-center block">Nhập mã OTP</Label>
              <OTPInput
                length={6}
                value={otp}
                onChange={(val) => setOtp(val)}
              />
            </div>

            {error && (
              <div className="p-3 rounded-lg bg-destructive/10 border border-destructive/20">
                <p className="text-sm text-destructive">{error}</p>
              </div>
            )}

            <Button
              type="submit"
              disabled={isLoading}
              className="w-full bg-gradient-to-r from-green-600 to-teal-600 hover:from-green-500 hover:to-teal-500 text-white font-semibold disabled:opacity-50">
              
              {isLoading ? (
                <>
                  <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                  Đang xác thực...
                </>
              ) : (
                "Xác thực"
              )}
            </Button>
          </form>

          <div className="flex items-center justify-center mt-4">
            <Button
              type="button"
              variant="outline"
              size="sm"
              disabled={isResending || countdown > 0}
              onClick={handleResend}
              className="text-sm">
              
              {countdown > 0
                ? `Gửi lại mã sau ${countdown}s`
                : isResending
                  ? "Đang gửi..."
                  : "Gửi lại mã OTP"}
            </Button>
          </div>

          <p className="text-center text-xs text-gray-600 dark:text-gray-400 mt-4">
            Đã có tài khoản?{" "}
            <a
              href={Routes.login}
              className="text-blue-600 dark:text-blue-400 hover:underline">
              
              Đăng nhập ngay
            </a>
          </p>
        </CardContent>
      </Card>
    </div>
  );
}
