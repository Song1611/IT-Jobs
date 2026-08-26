import { jobApi } from "./job.api";
import { companyApi } from "./company.api";

export const searchApi = {
  search: async (keyword, pageNumber = 1, pageSize = 10) => {
    try {
      const [jobsRes, companiesRes] = await Promise.all([
        jobApi.search(keyword, pageNumber, pageSize),
        companyApi.search(keyword, pageNumber, pageSize)
      ]);

      const jobs = jobsRes?.items || [];
      const companies = companiesRes?.items || [];

      return {
        success: true,
        data: {
          totalResults: jobs.length + companies.length,
          message: `Tìm thấy ${jobs.length + companies.length} kết quả`,
          searchType: 'job',
          jobs,
          companies
        }
      };
    } catch {
      return {
        success: true,
        data: {
          totalResults: 0,
          message: "",
          searchType: 'job',
          jobs: [],
          companies: []
        }
      };
    }
  }
};