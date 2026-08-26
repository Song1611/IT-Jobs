"use client";

import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useState } from "react";
import { useRouter } from "next/navigation";
import {
  Form,
  FormField,
  FormItem,
  FormLabel,
  FormControl,
  FormMessage } from
"@/components/ui/shadcn/form";
import { Input } from "@/components/ui/shadcn/input";
import { Button } from "@/components/ui/shadcn/button";
import {
  Card,
  CardHeader,
  CardTitle,
  CardContent } from
"@/components/ui/shadcn/card";
import { RegisterHRFormSchema } from "@/lib/validations/register.validation";
import { useAuth } from "@/components/providers/auth.provider";
import Routes from "@/constants/routes";

export default function FormRegisterHR() {
  const { registerHR } = useAuth();
  const router = useRouter();
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(false);

  const form = useForm({
    resolver: zodResolver(RegisterHRFormSchema),
    defaultValues: {
      fullName: "",
      email: "",
      password: ""
    }
  });

  const onSubmit = async (values) => {
    setError("");
    setIsLoading(true);

    const result = await registerHR(values);

    if (result.success) {
      router.push(`${Routes.verifyEmail}?email=${encodeURIComponent(values.email)}`);
    } else {
      setError(result.error || "Đăng ký thất bại. Vui lòng thử lại.");
    }

    setIsLoading(false);
  };

  return (
    <Card className="w-full max-w-md p-6 bg-white/70 dark:bg-gray-900/70 backdrop-blur-lg border border-gray-200 dark:border-gray-700 shadow-xl rounded-2xl">
      <CardHeader>
        <CardTitle className="text-2xl font-bold text-center text-gray-900 dark:text-gray-100">
          Đăng ký Nhà tuyển dụng
        </CardTitle>
      </CardHeader>

      <CardContent>
        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
            {/* Full Name */}
            <FormField
              control={form.control}
              name="fullName"
              render={({ field }) =>
              <FormItem>
                  <FormLabel className="text-sm">Họ và tên</FormLabel>
                  <FormControl>
                    <Input placeholder="Nguyễn Văn A" className="h-9" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              } />

            {/* Email */}
            <FormField
              control={form.control}
              name="email"
              render={({ field }) =>
              <FormItem>
                  <FormLabel className="text-sm">Email</FormLabel>
                  <FormControl>
                    <Input placeholder="hr@company.com" className="h-9" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              } />

            {/* Password */}
            <FormField
              control={form.control}
              name="password"
              render={({ field }) =>
              <FormItem>
                  <FormLabel className="text-sm">Mật khẩu</FormLabel>
                  <FormControl>
                    <Input type="password" placeholder="••••••••" className="h-9" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              } />

            {/* Error Message */}
            {error &&
            <div className="p-3 rounded-lg bg-destructive/10 border border-destructive/20">
                <p className="text-sm text-destructive">{error}</p>
              </div>
            }

            {/* Submit */}
            <Button
              type="submit"
              disabled={isLoading}
              className="w-full bg-gradient-to-r from-blue-600 to-purple-600 hover:from-blue-500 hover:to-purple-500 text-white font-semibold disabled:opacity-50">
              
              {isLoading ? "Đang đăng ký..." : "Đăng ký tài khoản HR"}
            </Button>

            {/* Link to user register */}
            <p className="text-center text-sm text-gray-600 dark:text-gray-400 mt-2">
              Bạn là ứng viên?{" "}
              <a
                href="/register"
                className="text-green-600 dark:text-green-400 hover:underline">
                
                Đăng ký tài khoản ứng viên
              </a>
            </p>

            {/* Link to login */}
            <p className="text-center text-sm text-gray-600 dark:text-gray-400">
              Đã có tài khoản?{" "}
              <a
                href="/login"
                className="text-blue-600 dark:text-blue-400 hover:underline">
                
                Đăng nhập ngay
              </a>
            </p>
          </form>
        </Form>
      </CardContent>
    </Card>);

}
