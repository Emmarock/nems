import { client } from './client'
import type {
  AccessEvent,
  AccessPolicy,
  PaymentAccount,
  PaymentAccountChange,
  AccessSubjectType,
  AccountBalance,
  Announcement,
  UserNotification,
  Complaint,
  ComplaintStatus,
  Dashboard,
  Gate,
  HouseholdMember,
  HouseholdMemberLookup,
  Invoice,
  LoginResponse,
  Levy,
  LevyBalance,
  MeDashboard,
  NotificationLog,
  PageResponse,
  Payment,
  Property,
  PropertyLookup,
  BulkCreateResidentUsersResult,
  ResendInviteResult,
  Resident,
  Role,
  ResidentArrears,
  ResidentLookup,
  RfidTag,
  RfidRequest,
  SecurityDashboard,
  StickerRequest,
  PaymentStatus,
  SharedReceipt,
  TenantAddResult,
  User,
  Vehicle,
  VehicleLookup,
  Visitor,
  VisitorLookup,
  VisitorScanResult,
  VisitorStatus,
  Worker,
  WorkerLookup,
} from './types'

export const authApi = {
  /** identifier is either the account's email or its registered phone number. */
  login: (identifier: string, password: string) =>
    client.post<LoginResponse>('/auth/login', { identifier, password }).then((r) => r.data),
  changePassword: (currentPassword: string, newPassword: string) =>
    client.put<void>('/auth/password', { currentPassword, newPassword }).then(() => undefined),
  /** The one-time link from an onboarding email — token is the credential, no prior login needed. */
  resetPassword: (token: string, newPassword: string) =>
    client.put<void>('/auth/reset-password', { token, newPassword }).then(() => undefined),
  /**
   * "Forgot password?" for anyone who already has a login but can't remember it — sends a
   * one-time reset link to whatever real contact channels the account has on file. Always
   * resolves the same way regardless of whether the identifier matched anything, see
   * UserService.startForgotPassword.
   */
  forgotPassword: (identifier: string) =>
    client.post<void>('/auth/forgot-password', { identifier }).then(() => undefined),
  /**
   * Self-service recovery for an existing resident with no login (or who lost access to it).
   * Step 1: sends a WhatsApp code to the phone on file — always resolves the same way regardless
   * of whether anything matched, see UserService.startClaimAccount.
   */
  claimAccountStart: (phone: string) =>
    client.post<void>('/auth/claim-account/start', { phone }).then(() => undefined),
  /** Step 2: verifies the WhatsApp code and, on success, emails a reset link — see UserService.verifyClaimAccount. */
  claimAccountVerify: (phone: string, code: string, email: string) =>
    client.post<void>('/auth/claim-account/verify', { phone, code, email }).then(() => undefined),
}

export const usersApi = {
  list: (params: { q?: string; page?: number; size?: number } = {}) =>
    client.get<PageResponse<User>>('/users', { params }).then((r) => r.data),
  create: (body: { email: string; phone?: string; password: string; fullName: string; role: string; residentId?: number }) =>
    client.post<User>('/users', body).then((r) => r.data),
  setStatus: (id: number, status: 'ACTIVE' | 'DISABLED') =>
    client.put<User>(`/users/${id}/status`, null, { params: { status } }).then((r) => r.data),
  resetPassword: (id: number, newPassword: string) =>
    client.put<void>(`/users/${id}/password`, { newPassword }).then(() => undefined),
  bulkCreateResidents: (residentIds?: number[]) =>
    client
      .post<BulkCreateResidentUsersResult>('/users/bulk-create-residents', { residentIds })
      .then((r) => r.data),
  resendInvite: (id: number) => client.post<ResendInviteResult>(`/users/${id}/resend-invite`).then((r) => r.data),
}

export const residentsApi = {
  list: (params: { q?: string; propertyId?: number; page?: number; size?: number } = {}) =>
    client.get<PageResponse<Resident>>('/residents', { params }).then((r) => r.data),
  get: (id: number) => client.get<Resident>(`/residents/${id}`).then((r) => r.data),
  create: (body: Omit<Resident, 'id' | 'status' | 'registrationDate' | 'propertyHouseNumber'>) =>
    client.post<Resident>('/residents', body).then((r) => r.data),
  update: (id: number, body: Omit<Resident, 'id' | 'status' | 'registrationDate' | 'propertyHouseNumber'>) =>
    client.put<Resident>(`/residents/${id}`, body).then((r) => r.data),
  lookup: (qrToken: string) => client.get<ResidentLookup>(`/residents/lookup/${qrToken}`).then((r) => r.data),
  arrears: (params: { q?: string; page?: number; size?: number } = {}) =>
    client.get<PageResponse<ResidentArrears>>('/residents/arrears', { params }).then((r) => r.data),
  /** Downloadable PDF snapshot of every account currently in arrears — same list as arrears(), unpaginated. */
  arrearsReport: () =>
    client.get('/residents/arrears/report', { responseType: 'blob' }).then((r) => r.data as Blob),
  /** Disables login and revokes any household passes the resident sponsors — see ResidentService.suspend. */
  suspend: (id: number) => client.post<Resident>(`/residents/${id}/suspend`).then((r) => r.data),
  reactivate: (id: number) => client.post<Resident>(`/residents/${id}/reactivate`).then((r) => r.data),
  checkIn: (qrToken: string, gateId?: number) =>
    client.post<Resident>(`/residents/checkin/${qrToken}`, null, { params: { gateId } }).then((r) => r.data),
  checkOut: (qrToken: string, gateId?: number) =>
    client.post<Resident>(`/residents/checkout/${qrToken}`, null, { params: { gateId } }).then((r) => r.data),
}

export const propertiesApi = {
  list: (params: { q?: string; page?: number; size?: number } = {}) =>
    client.get<PageResponse<Property>>('/properties', { params }).then((r) => r.data),
  get: (id: number) => client.get<Property>(`/properties/${id}`).then((r) => r.data),
  create: (body: Omit<Property, 'id' | 'ownerName'>) => client.post<Property>('/properties', body).then((r) => r.data),
  update: (id: number, body: Omit<Property, 'id' | 'ownerName'>) =>
    client.put<Property>(`/properties/${id}`, body).then((r) => r.data),
  accessPass: (id: number) => client.get<{ qrToken: string }>(`/properties/${id}/access-pass`).then((r) => r.data.qrToken),
  lookup: (qrToken: string) => client.get<PropertyLookup>(`/properties/lookup/${qrToken}`).then((r) => r.data),
}

export const vehiclesApi = {
  list: (params: { q?: string; residentId?: number; page?: number; size?: number } = {}) =>
    client.get<PageResponse<Vehicle>>('/vehicles', { params }).then((r) => r.data),
  get: (id: number) => client.get<Vehicle>(`/vehicles/${id}`).then((r) => r.data),
  create: (body: Omit<Vehicle, 'id' | 'status' | 'residentName'>) => client.post<Vehicle>('/vehicles', body).then((r) => r.data),
  accessPass: (id: number) => client.get<{ qrToken: string }>(`/vehicles/${id}/access-pass`).then((r) => r.data.qrToken),
  /** Landlord self-service: tenant-registered vehicles on the caller's own property awaiting approval. */
  pendingApproval: (params: { page?: number; size?: number } = {}) =>
    client.get<PageResponse<Vehicle>>('/vehicles/pending-approval', { params }).then((r) => r.data),
  approve: (id: number) => client.put<Vehicle>(`/vehicles/${id}/approve`).then((r) => r.data),
  lookup: (qrToken: string) => client.get<VehicleLookup>(`/vehicles/lookup/${qrToken}`).then((r) => r.data),
  checkIn: (qrToken: string, gateId?: number) =>
    client.post<Vehicle>(`/vehicles/checkin/${qrToken}`, null, { params: { gateId } }).then((r) => r.data),
  checkOut: (qrToken: string, gateId?: number) =>
    client.post<Vehicle>(`/vehicles/checkout/${qrToken}`, null, { params: { gateId } }).then((r) => r.data),
}

export const leviesApi = {
  list: (params: { page?: number; size?: number } = {}) =>
    client.get<PageResponse<Levy>>('/levies', { params }).then((r) => r.data),
  create: (body: Omit<Levy, 'id' | 'paymentAccountLabel'>) => client.post<Levy>('/levies', body).then((r) => r.data),
  update: (id: number, body: Omit<Levy, 'id' | 'paymentAccountLabel'>) =>
    client.put<Levy>(`/levies/${id}`, body).then((r) => r.data),
}

export const invoicesApi = {
  list: (params: { q?: string; residentId?: number; page?: number; size?: number } = {}) =>
    client.get<PageResponse<Invoice>>('/invoices', { params }).then((r) => r.data),
  generate: (body: { residentId: number; levyId: number; dueDate?: string }) =>
    client.post<Invoice>('/invoices', body).then((r) => r.data),
  cancel: (id: number) => client.put<Invoice>(`/invoices/${id}/cancel`).then((r) => r.data),
}

export const accountsApi = {
  balance: (residentId: number) => client.get<AccountBalance>(`/accounts/${residentId}/balance`).then((r) => r.data),
  balanceBreakdown: (residentId: number) =>
    client.get<LevyBalance[]>(`/accounts/${residentId}/balance-breakdown`).then((r) => r.data),
  invoices: (residentId: number, params: { page?: number; size?: number } = {}) =>
    client.get<PageResponse<Invoice>>(`/accounts/${residentId}/invoices`, { params }).then((r) => r.data),
  payments: (residentId: number, params: { page?: number; size?: number } = {}) =>
    client.get<PageResponse<Payment>>(`/accounts/${residentId}/payments`, { params }).then((r) => r.data),
}

export const paymentsApi = {
  list: (params: { q?: string; residentId?: number; status?: PaymentStatus; page?: number; size?: number } = {}) =>
    client.get<PageResponse<Payment>>('/payments', { params }).then((r) => r.data),
  recordManual: (body: { residentId: number; invoiceId?: number; amount: number; method: string }) =>
    client.post<Payment>('/payments', body).then((r) => r.data),
  approve: (id: number, notes?: string) => client.put<Payment>(`/payments/${id}/approve`, { notes }).then((r) => r.data),
  reject: (id: number, notes?: string) => client.put<Payment>(`/payments/${id}/reject`, { notes }).then((r) => r.data),
}

export const meApi = {
  dashboard: () => client.get<MeDashboard>('/me').then((r) => r.data),
  balance: () => client.get<AccountBalance>('/me/account/balance').then((r) => r.data),
  balanceBreakdown: () => client.get<LevyBalance[]>('/me/account/balance-breakdown').then((r) => r.data),
  invoices: (params: { page?: number; size?: number } = {}) =>
    client.get<PageResponse<Invoice>>('/me/account/invoices', { params }).then((r) => r.data),
  payments: (params: { page?: number; size?: number } = {}) =>
    client.get<PageResponse<Payment>>('/me/account/payments', { params }).then((r) => r.data),
  submitReceipt: (body: { levyId: number; amount: number; method: string; receiptImage?: string }) =>
    client.post<Payment>('/me/payments/receipts', body).then((r) => r.data),
  vehicles: (params: { page?: number; size?: number } = {}) =>
    client.get<PageResponse<Vehicle>>('/me/vehicles', { params }).then((r) => r.data),
  registerVehicle: (body: Omit<Vehicle, 'id' | 'status' | 'residentId' | 'residentName'>) =>
    client.post<Vehicle>('/me/vehicles', body).then((r) => r.data),
  vehicleAccessPass: (id: number) =>
    client.get<{ qrToken: string }>(`/me/vehicles/${id}/access-pass`).then((r) => r.data.qrToken),
  accessPass: () => client.get<{ qrToken: string }>('/me/access-pass').then((r) => r.data.qrToken),
  updateProfile: (body: { fullName: string; phone: string; email?: string; emergencyContact?: string }) =>
    client.put<Resident>('/me/profile', body).then((r) => r.data),
  updateProperty: (body: { houseNumber: string; address?: string; road: string; postcode?: string }) =>
    client.put<Property>('/me/property', body).then((r) => r.data),
  /** Landlord self-service — caps how many ACTIVE tenants can be added (see /me/tenants, meApi.addTenant). */
  updateApartmentCount: (apartmentCount: number | null) =>
    client.put<Property>('/me/landlord/apartment-count', { apartmentCount }).then((r) => r.data),
  tenants: (params: { page?: number; size?: number } = {}) =>
    client.get<PageResponse<Resident>>('/me/tenants', { params }).then((r) => r.data),
  addTenant: (body: { fullName: string; phone: string; email?: string; emergencyContact?: string }) =>
    client.post<TenantAddResult>('/me/tenants', body).then((r) => r.data),
  deactivateTenant: (id: number) => client.put<Resident>(`/me/tenants/${id}/deactivate`).then((r) => r.data),
  /** Tenant self-service: their landlord's account, read-only — billing lives on the landlord's account, not theirs. */
  landlordBalance: () => client.get<AccountBalance>('/me/landlord/account/balance').then((r) => r.data),
  landlordBalanceBreakdown: () =>
    client.get<LevyBalance[]>('/me/landlord/account/balance-breakdown').then((r) => r.data),
  landlordInvoices: (params: { page?: number; size?: number } = {}) =>
    client.get<PageResponse<Invoice>>('/me/landlord/account/invoices', { params }).then((r) => r.data),
  landlordPayments: (params: { page?: number; size?: number } = {}) =>
    client.get<PageResponse<Payment>>('/me/landlord/account/payments', { params }).then((r) => r.data),
  /** What a tenant does instead of submitReceipt — hands it to their landlord to review and submit. */
  shareReceipt: (body: { levyId: number; amount: number; method: string; receiptImage?: string }) =>
    client.post<SharedReceipt>('/me/receipts/share', body).then((r) => r.data),
  /** Landlord self-service: receipts tenants have shared, awaiting review. */
  sharedReceipts: (params: { page?: number; size?: number } = {}) =>
    client.get<PageResponse<SharedReceipt>>('/me/receipts/shared', { params }).then((r) => r.data),
  submitSharedReceipt: (id: number) => client.post<Payment>(`/me/receipts/shared/${id}/submit`).then((r) => r.data),
  dismissSharedReceipt: (id: number) => client.post<void>(`/me/receipts/shared/${id}/dismiss`).then(() => undefined),
}

export const visitorsApi = {
  mine: (params: { page?: number; size?: number } = {}) =>
    client.get<PageResponse<Visitor>>('/visitors/mine', { params }).then((r) => r.data),
  listAll: (params: { q?: string; status?: VisitorStatus; page?: number; size?: number } = {}) =>
    client.get<PageResponse<Visitor>>('/visitors', { params }).then((r) => r.data),
  get: (id: number) => client.get<Visitor>(`/visitors/${id}`).then((r) => r.data),
  create: (body: { name: string; phone: string; vehiclePlate?: string; validFrom: string; validUntil: string; photo?: string }) =>
    client.post<Visitor>('/visitors', body).then((r) => r.data),
  cancel: (id: number) => client.put<Visitor>(`/visitors/${id}/cancel`).then((r) => r.data),
  lookup: (qrToken: string) => client.get<VisitorLookup>(`/visitors/lookup/${qrToken}`).then((r) => r.data),
  /** Checks the visitor in or out — which one is fixed by the scanned token, not a choice. */
  scan: (qrToken: string, gateId?: number) =>
    client.post<VisitorScanResult>(`/visitors/scan/${qrToken}`, null, { params: { gateId } }).then((r) => r.data),
}

export const workersApi = {
  mine: (params: { page?: number; size?: number } = {}) =>
    client.get<PageResponse<Worker>>('/workers/mine', { params }).then((r) => r.data),
  listAll: (params: { q?: string; activeOnly?: boolean; page?: number; size?: number } = {}) =>
    client.get<PageResponse<Worker>>('/workers', { params }).then((r) => r.data),
  /** Admin filter: due (or overdue) for checkout by their own contract end date, but still checked in right now. */
  dueCheckoutOnSite: (params: { page?: number; size?: number } = {}) =>
    client.get<PageResponse<Worker>>('/workers/due-checkout-on-site', { params }).then((r) => r.data),
  listBySponsor: (sponsorResidentId: number, params: { q?: string; page?: number; size?: number } = {}) =>
    client.get<PageResponse<Worker>>('/workers', { params: { ...params, sponsorResidentId } }).then((r) => r.data),
  get: (id: number) => client.get<Worker>(`/workers/${id}`).then((r) => r.data),
  request: (body: {
    fullName: string
    phone: string
    nationalId?: string
    contractorName: string
    workType: string
    workerCategory: Worker['workerCategory']
    startDate: string
    expectedEndDate: string
    photo?: string
  }) => client.post<Worker>('/workers', body).then((r) => r.data),
  update: (
    id: number,
    body: {
      fullName: string
      phone: string
      nationalId?: string
      contractorName: string
      workType: string
      workerCategory: Worker['workerCategory']
      startDate: string
      expectedEndDate: string
      photo?: string
    },
  ) => client.put<Worker>(`/workers/${id}`, body).then((r) => r.data),
  suspend: (id: number) => client.post<Worker>(`/workers/${id}/suspend`).then((r) => r.data),
  complete: (id: number) => client.post<Worker>(`/workers/${id}/complete`).then((r) => r.data),
  logs: (id: number, params: { page?: number; size?: number } = {}) =>
    client.get<PageResponse<AccessEvent>>(`/workers/${id}/logs`, { params }).then((r) => r.data),
  lookup: (qrToken: string) => client.get<WorkerLookup>(`/workers/lookup/${qrToken}`).then((r) => r.data),
  checkIn: (qrToken: string, gateId?: number) =>
    client.post<Worker>(`/workers/checkin/${qrToken}`, null, { params: { gateId } }).then((r) => r.data),
  checkOut: (qrToken: string, gateId?: number) =>
    client.post<Worker>(`/workers/checkout/${qrToken}`, null, { params: { gateId } }).then((r) => r.data),
}

export const householdMembersApi = {
  mine: (params: { page?: number; size?: number } = {}) =>
    client.get<PageResponse<HouseholdMember>>('/household-members/mine', { params }).then((r) => r.data),
  /** Household members awaiting the caller's (landlord's) approval — empty if they own no property. */
  pendingApproval: (params: { page?: number; size?: number } = {}) =>
    client.get<PageResponse<HouseholdMember>>('/household-members/pending-approval', { params }).then((r) => r.data),
  listAll: (params: { q?: string; propertyId?: number; sponsorResidentId?: number; page?: number; size?: number } = {}) =>
    client.get<PageResponse<HouseholdMember>>('/household-members', { params }).then((r) => r.data),
  get: (id: number) => client.get<HouseholdMember>(`/household-members/${id}`).then((r) => r.data),
  request: (body: { fullName: string; phone: string; relationship: string; photo?: string }) =>
    client.post<HouseholdMember>('/household-members', body).then((r) => r.data),
  approve: (id: number) => client.put<HouseholdMember>(`/household-members/${id}/approve`).then((r) => r.data),
  revoke: (id: number) => client.put<HouseholdMember>(`/household-members/${id}/revoke`).then((r) => r.data),
  logs: (id: number, params: { page?: number; size?: number } = {}) =>
    client.get<PageResponse<AccessEvent>>(`/household-members/${id}/logs`, { params }).then((r) => r.data),
  lookup: (qrToken: string) => client.get<HouseholdMemberLookup>(`/household-members/lookup/${qrToken}`).then((r) => r.data),
  checkIn: (qrToken: string, gateId?: number) =>
    client.post<HouseholdMember>(`/household-members/checkin/${qrToken}`, null, { params: { gateId } }).then((r) => r.data),
  checkOut: (qrToken: string, gateId?: number) =>
    client.post<HouseholdMember>(`/household-members/checkout/${qrToken}`, null, { params: { gateId } }).then((r) => r.data),
}

export const complaintsApi = {
  get: (id: number) => client.get<Complaint>(`/complaints/${id}`).then((r) => r.data),
  mine: (params: { page?: number; size?: number } = {}) =>
    client.get<PageResponse<Complaint>>('/complaints/mine', { params }).then((r) => r.data),
  listAll: (params: { status?: ComplaintStatus; openOnly?: boolean; page?: number; size?: number } = {}) =>
    client.get<PageResponse<Complaint>>('/complaints', { params }).then((r) => r.data),
  listByResident: (residentId: number, params: { page?: number; size?: number } = {}) =>
    client.get<PageResponse<Complaint>>('/complaints', { params: { ...params, residentId } }).then((r) => r.data),
  create: (body: { category: string; description: string; priority?: string }) =>
    client.post<Complaint>('/complaints', body).then((r) => r.data),
  updateStatus: (id: number, status: ComplaintStatus) =>
    client.put<Complaint>(`/complaints/${id}/status`, { status }).then((r) => r.data),
  /** Routes the complaint to a staff role (security, finance, treasurer, ...) - pass null to clear it. */
  assign: (id: number, assignedRole: Role | null) =>
    client.put<Complaint>(`/complaints/${id}/assign`, { assignedRole }).then((r) => r.data),
}

export const notificationLogsApi = {
  list: (params: { channel?: string; status?: string; q?: string; page?: number; size?: number } = {}) =>
    client.get<PageResponse<NotificationLog>>('/notification-logs', { params }).then((r) => r.data),
}

export const announcementsApi = {
  list: (params: { page?: number; size?: number } = {}) =>
    client.get<PageResponse<Announcement>>('/announcements', { params }).then((r) => r.data),
  create: (body: { title: string; message: string; channels: string[] }) =>
    client.post<Announcement>('/announcements', body).then((r) => r.data),
  unreadCount: () => client.get<{ unread: number }>('/announcements/unread-count').then((r) => r.data.unread),
  markRead: (id: number) => client.post<void>(`/announcements/${id}/read`).then(() => undefined),
  markUnread: (id: number) => client.post<void>(`/announcements/${id}/unread`).then(() => undefined),
}

export const userNotificationsApi = {
  mine: (params: { page?: number; size?: number } = {}) =>
    client.get<PageResponse<UserNotification>>('/notifications', { params }).then((r) => r.data),
  unreadCount: () => client.get<{ unread: number }>('/notifications/unread-count').then((r) => r.data.unread),
  markRead: (id: number) => client.post<void>(`/notifications/${id}/read`).then(() => undefined),
  markUnread: (id: number) => client.post<void>(`/notifications/${id}/unread`).then(() => undefined),
}

export const reportsApi = {
  dashboard: () => client.get<Dashboard>('/reports/dashboard').then((r) => r.data),
}

export const gatesApi = {
  list: (params: { page?: number; size?: number } = {}) =>
    client.get<PageResponse<Gate>>('/gates', { params }).then((r) => r.data),
  get: (id: number) => client.get<Gate>(`/gates/${id}`).then((r) => r.data),
  create: (body: Omit<Gate, 'id' | 'status'>) => client.post<Gate>('/gates', body).then((r) => r.data),
  update: (id: number, body: Omit<Gate, 'id'>) => client.put<Gate>(`/gates/${id}`, body).then((r) => r.data),
}

export const accessPolicyApi = {
  get: () => client.get<AccessPolicy>('/access-policy').then((r) => r.data),
  update: (body: AccessPolicy) => client.put<AccessPolicy>('/access-policy', body).then((r) => r.data),
}

export const paymentAccountApi = {
  list: () => client.get<PaymentAccount[]>('/payment-account').then((r) => r.data),
  listPending: () => client.get<PaymentAccountChange[]>('/payment-account/pending').then((r) => r.data),
  propose: (body: { targetAccountId?: number; label: string; bankName: string; accountNumber: string; accountName: string }) =>
    client.post<PaymentAccountChange>('/payment-account/changes', body).then((r) => r.data),
  approve: (id: number, notes?: string) =>
    client.put<PaymentAccountChange>(`/payment-account/changes/${id}/approve`, { notes }).then((r) => r.data),
  reject: (id: number, notes?: string) =>
    client.put<PaymentAccountChange>(`/payment-account/changes/${id}/reject`, { notes }).then((r) => r.data),
}

export const accessEventsApi = {
  list: (params: { subjectType?: AccessSubjectType; subjectId?: number; page?: number; size?: number } = {}) =>
    client.get<PageResponse<AccessEvent>>('/access-events', { params }).then((r) => r.data),
  listByGate: (gateId: number, params: { page?: number; size?: number } = {}) =>
    client.get<PageResponse<AccessEvent>>('/access-events', { params: { ...params, gateId } }).then((r) => r.data),
  record: (body: { subjectType: AccessSubjectType; subjectId: number; direction: 'IN' | 'OUT'; gateId?: number }) =>
    client.post<AccessEvent>('/access-events', body).then((r) => r.data),
}

export const rfidApi = {
  list: (params: { page?: number; size?: number } = {}) =>
    client.get<PageResponse<RfidTag>>('/rfid', { params }).then((r) => r.data),
  issue: (body: {
    tagId: string
    assignedResidentId?: number
    assignedWorkerId?: number
    assignedVisitorId?: number
    vehicleId?: number
  }) => client.post<RfidTag>('/rfid', body).then((r) => r.data),
  revoke: (id: number) => client.put<RfidTag>(`/rfid/${id}/revoke`).then((r) => r.data),
  markLost: (id: number) => client.put<RfidTag>(`/rfid/${id}/lost`).then((r) => r.data),
}

export const rfidRequestsApi = {
  create: (body: { subjectType: 'VISITOR' | 'WORKER'; subjectId: number }) =>
    client.post<RfidRequest>('/rfid-requests', body).then((r) => r.data),
  mine: (params: { page?: number; size?: number } = {}) =>
    client.get<PageResponse<RfidRequest>>('/rfid-requests/mine', { params }).then((r) => r.data),
  cancel: (id: number) => client.put<RfidRequest>(`/rfid-requests/${id}/cancel`).then((r) => r.data),
  pending: (params: { page?: number; size?: number } = {}) =>
    client.get<PageResponse<RfidRequest>>('/rfid-requests/pending', { params }).then((r) => r.data),
  issue: (id: number, tagId: string) =>
    client.post<RfidRequest>(`/rfid-requests/${id}/issue`, { tagId }).then((r) => r.data),
}

export const stickerRequestsApi = {
  mine: (params: { page?: number; size?: number } = {}) =>
    client.get<PageResponse<StickerRequest>>('/sticker-requests/mine', { params }).then((r) => r.data),
  list: (params: { residentId?: number; page?: number; size?: number } = {}) =>
    client.get<PageResponse<StickerRequest>>('/sticker-requests', { params }).then((r) => r.data),
  request: (vehicleId: number) =>
    client.post<StickerRequest>('/sticker-requests', { vehicleId }).then((r) => r.data),
}

export const securityDashboardApi = {
  get: () => client.get<SecurityDashboard>('/security/dashboard').then((r) => r.data),
}
