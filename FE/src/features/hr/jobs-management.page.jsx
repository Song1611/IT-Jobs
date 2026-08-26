"use client";

import { useState, useEffect } from "react";
import { Badge } from "@/components/ui/shadcn/badge";
import { Button } from "@/components/ui/shadcn/button";
import { DataTable } from "@/components/cards/data-table.card";
import { jobApi } from "@/services/job.api";
import { JobDetailModal } from "@/components/modals/job-detail.modal";
import { CreateJobForm, CreateJobData } from "@/components/forms/create-job.form";
import { useAuth } from "@/components/providers/auth.provider";
import { ManagementTableSkeleton } from "@/components/ui/skeletons";
























const JobsManagement = () => {
  const { user, company, token } = useAuth();
  const [jobs, setJobs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [totalItems, setTotalItems] = useState(0);

  // Modal state
  const [modalOpen, setModalOpen] = useState(false);
  const [modalMode, setModalMode] = useState("view");
  const [selectedJob, setSelectedJob] = useState(null);

  // Create job modal
  const [createModalOpen, setCreateModalOpen] = useState(false);

  // Edit form
  const [editForm, setEditForm] = useState({
    companyId: 0,
    title: "",
    description: "",
    type: "",
    quantity: 0,
    deadline: "",
    status: ""
  });

  useEffect(() => {
    if (user?.id) {
      fetchJobs();
    }
  }, [user?.id]);

  const fetchJobs = async () => {
    try {
      setLoading(true);

      if (!user?.id) {
        console.error("No user found");
        setLoading(false);
        return;
      }

      const cId = company?.id || user.id;
      const response = await jobApi.getByUser(cId, 1, 50);

      if (response && response.items) {
        setJobs(response.items);
        setTotalItems(response.totalElements || 0);
      } else if (Array.isArray(response)) {
        setJobs(response);
        setTotalItems(response.length);
      }
    } catch (error) {
      console.error("❌ Error fetching jobs:", error);
    } finally {
      setLoading(false);
    }
  };

  const getStatusBadge = (status) => {
    const statusConfig = {
      open: {
        variant: "default",
        label: "Đang Mở"
      },
      draft: {
        variant: "secondary",
        label: "Bản Nháp"
      },
      closed: {
        variant: "destructive",
        label: "Đã Đóng"
      }
    };

    const config =
    statusConfig[status] || statusConfig.draft;

    return (
      <Badge variant={config.variant} className="text-xs whitespace-nowrap">
        {config.label}
      </Badge>);

  };

  const getTypeBadge = (type) => {
    const typeLabels = {
      "full-time": "Toàn thời gian",
      "part-time": "Bán thời gian",
      contract: "Hợp đồng",
      internship: "Thực tập"
    };

    return (
      <Badge variant="outline" className="text-xs whitespace-nowrap">
        {typeLabels[type] || type}
      </Badge>);

  };

  const formatDate = (dateString) => {
    return new Date(dateString).toLocaleDateString("vi-VN", {
      day: "2-digit",
      month: "2-digit",
      year: "numeric"
    });
  };

  const columns = [
  {
    key: "title",
    header: "Công Việc",
    sortable: true,
    render: (value, row) =>
    <div className="space-y-0.5 min-w-[150px] max-w-[200px]">
          <div className="font-medium text-xs line-clamp-1">{value}</div>
          <div className="text-[10px] text-muted-foreground line-clamp-1">
            {row.description}
          </div>
        </div>

  },
  {
    key: "type",
    header: "Loại",
    sortable: true,
    render: (value) => getTypeBadge(value)
  },
  {
    key: "status",
    header: "TT",
    sortable: true,
    render: (value) => getStatusBadge(value)
  },
  {
    key: "quantity",
    header: "SL",
    sortable: true,
    render: (value) => <span className="font-bold text-xs">{value}</span>
  },
  {
    key: "skills",
    header: "Kỹ Năng",
    sortable: false,
    render: (value) =>
    <div className="flex gap-1 max-w-[100px] flex-wrap">
          {value.slice(0, 1).map((skill) =>
      <Badge
        key={skill.id}
        variant="outline"
        className="text-[10px] px-1 py-0">
        
              {skill.name}
            </Badge>
      )}
          {value.length > 1 &&
      <Badge variant="outline" className="text-[10px] px-1 py-0">
              +{value.length - 1}
            </Badge>
      }
        </div>

  },
  {
    key: "deadline",
    header: "Hạn",
    sortable: true,
    render: (value) =>
    <span className="text-[10px] whitespace-nowrap">{formatDate(value)}</span>

  }];


  const filterOptions = [
  { value: "open", label: "Đang Mở" },
  { value: "closed", label: "Đã Đóng" }];


  const jobActions = [
  { key: "view", label: "Xem", className: "text-blue-600" },
  { key: "edit", label: "Sửa", className: "text-orange-600" },
  { key: "delete", label: "Gỡ Bỏ", className: "text-destructive" }];


  const handleViewJob = async (jobId) => {
    try {
      const response = await jobApi.getById(jobId);
      console.log("📦 Job Detail:", response);
      setSelectedJob(response);
      setModalMode("view");
      setModalOpen(true);
    } catch (error) {
      console.error("❌ Error fetching job detail:", error);
      alert("Không thể tải thông tin công việc");
    }
  };

  const handleEditJob = async (job) => {
    try {
      const response = await jobApi.getById(job.id);
      const jobDetail = response;

      setSelectedJob(jobDetail);
      setEditForm({
        companyId: jobDetail.company.id,
        title: jobDetail.title,
        description: jobDetail.description,
        type: jobDetail.type,
        quantity: jobDetail.quantity,
        deadline: jobDetail.deadline.split("T")[0],
        status: jobDetail.status
      });
      setModalMode("edit");
      setModalOpen(true);
    } catch (error) {
      alert("Không thể tải thông tin công việc");
    }
  };

  const handleCreateJob = async (data) => {
    if (!user?.id || !token) {
      alert("Vui lòng đăng nhập để thực hiện chức năng này");
      return;
    }

    const cId = company?.id || user.id;
    try {
      await jobApi.create(cId, data);
      alert("Đăng tin tuyển dụng thành công!");
      fetchJobs();
    } catch (error) {
      alert("Đăng tin thất bại!");
      throw error;
    }
  };

  const handleSaveEdit = async () => {
    if (!selectedJob || !token) return;

    const cId = company?.id || editForm.companyId;
    try {
      await jobApi.update(selectedJob.id, cId, editForm);
      alert("Cập nhật thành công!");
      setModalOpen(false);
      fetchJobs();
    } catch (error) {
      alert("Cập nhật thất bại!");
    }
  };

  const handleDeleteJob = async (job) => {
    if (!confirm(`Bạn có chắc muốn gỡ bỏ tin "${job.title}"?`)) return;

    if (!token) {
      alert("Không tìm thấy token xác thực");
      return;
    }

    try {
      const cId = company?.id || user.id;
      await jobApi.delete(job.id, cId);
      alert("Đã gỡ bỏ thành công!");
      fetchJobs();
    } catch (error) {
      alert("Gỡ bỏ thất bại!");
    }
  };

  const handleRowAction = (action, job) => {
    switch (action) {
      case "view":
        handleViewJob(job.id);
        break;
      case "edit":
        handleEditJob(job);
        break;
      case "delete":
        handleDeleteJob(job);
        break;
    }
  };

  if (loading) {
    return <ManagementTableSkeleton />;
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div className="space-y-2">
          <h1 className="text-3xl font-bold font-mono">Quản Lý Công Việc</h1>
          <p className="text-muted-foreground">Quản lý {totalItems} tin tuyển dụng</p>
        </div>
        <Button
          className="font-mono"
          onClick={() => setCreateModalOpen(true)}>
          
          Đăng Tin Tuyển Dụng
        </Button>
      </div>

      <DataTable
        data={jobs}
        columns={columns}
        searchKey="title"
        filterKey="status"
        filterOptions={filterOptions}
        actions={jobActions}
        onRowAction={handleRowAction} />
      

      {/* Create Job Modal */}
      <CreateJobForm
        open={createModalOpen}
        onOpenChange={setCreateModalOpen}
        onSubmit={handleCreateJob}
        companyId={user?.id || 0} />
      

      {/* Job Detail Modal */}
      <JobDetailModal
        open={modalOpen}
        onOpenChange={setModalOpen}
        mode={modalMode}
        job={selectedJob}
        form={editForm}
        onFormChange={setEditForm}
        onSave={handleSaveEdit} />
      
    </div>);

};

export default JobsManagement;