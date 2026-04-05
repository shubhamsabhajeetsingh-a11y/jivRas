export interface UserProfile {
  username: string;
  role: 'SUPER_ADMIN' | 'ADMIN' | 'BRANCH_MANAGER' | 'EMPLOYEE';
  firstName: string;
  lastName: string;
  email: string;
  branchId: number; // 0 for ADMIN (no specific branch), positive for EMPLOYEE/BRANCH_MANAGER
}

export interface Branch {
  id: number;
  name: string;
  address: string;
  pincode: string;
  city: string;
  managerUsername: string;
  active: boolean;
}

export interface BranchInventoryResponse {
  id: number;
  branchId: number;
  branchName: string;
  productId: number;
  productName: string;
  pricePerKg: number;
  imageUrl: string;
  availableStockKg: number;
  lowStockThreshold: number;
  lowStock: boolean; // API determines this based on threshold

  // Computed client-side in filterProducts() — not from API
  computedStatus?: { label: string; cls: string };
  computedCategory?: string;
}

export interface BranchInventoryRequest {
  branchId: number;
  productId: number;
  availableStockKg: number;
  lowStockThreshold: number;
}
