"use client";

import React, { ReactNode } from "react";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogTrigger } from
"@/components/ui/shadcn/dialog";







export default function ListDialog({ trigger, title, children }) {
  return (
    <Dialog>
      <DialogTrigger asChild>{trigger}</DialogTrigger>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <DialogTitle>{title}</DialogTitle>
        </DialogHeader>
        <div className="max-h-[400px] overflow-y-auto pr-4">
          {children}
        </div>
      </DialogContent>
    </Dialog>);

}