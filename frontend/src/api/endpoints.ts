import { client } from './client'
import type {
  AccessEvent,
  AccessPolicy,
  AccessSubjectType,
  AccountBalance,
  Announcement,
  Complaint,
  ComplaintStatus,
  Dashboard,
  Gate,
  Invoice,
  LoginResponse,
  Levy,
  LevyBalance,
  MeDashboard,
  PageResponse,
  Payment,
  Property,
  PropertyLookup,
  BulkCreateResidentUsersResult,
  Resident,
  ResidentArrears,
  ResidentLookup,
  RfidTag,
  SecurityDashboard,
  User,
  Vehicle,
  VehicleLookup,
  Visitor,
  VisitorLookup,
  Worker,
  WorkerLookup,
} from './types'

export const authApi = {
  /** identifier is either the account's email or its registered phone number. */
  login: (identifier: string, password: string) =>
    client.post<LoginResponse>('/auth/login', { identifier, password }).then((r) => r.data),
  changePassword: (currentPassword: string, newPassword: string) =>
    client.put<void>('/auth/password', { currentPassword, newPassword }).then(() => undefined),
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
  lookup: (qrToken: string) => client.get<VehicleLookup>(`/vehicles/lookup/${qrToken}`).then((r) => r.data),
  checkIn: (qrToken: string, gateId?: number) =>
    client.post<Vehicle>(`/vehicles/checkin/${qrToken}`, null, { params: { gateId } }).then((r) => r.data),
  checkOut: (qrToken: string, gateId?: number) =>
    client.post<Vehicle>(`/vehicles/checkout/${qrToken}`, null, { params: { gateId } }).then((r) => r.data),
}

export const leviesApi = {
  list: (params: { page?: number; size?: number } = {}) =>
    client.get<PageResponse<Levy>>('/levies', { params }).then((r) => r.data),
  create: (body: Omit<Levy, 'id'>) => client.post<Levy>('/levies', body).then((r) => r.data),
  update: (id: number, body: Omit<Levy, 'id'>) => client.put<Levy>(`/levies/${id}`, body).then((r) => r.data),
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
  list: (params: { q?: string; residentId?: number; page?: number; size?: number } = {}) =>
    client.get<PageResponse<Payment>>('/payments', { params }).then((r) => r.data),
  recordManual: (body: { residentId: number; invoiceId?: number; amount: number; method: string }) =>
    client.post<Payment>('/payments', body).then((r) => r.data),
  webhook: (providerReference: string, status: string) =>
    client.post<Payment>('/payments/webhook', { providerReference, status }).then((r) => r.data),
}

export const meApi = {
  dashboard: () => client.get<MeDashboard>('/me').then((r) => r.data),
  balance: () => client.get<AccountBalance>('/me/account/balance').then((r) => r.data),
  balanceBreakdown: () => client.get<LevyBalance[]>('/me/account/balance-breakdown').then((r) => r.data),
  invoices: (params: { page?: number; size?: number } = {}) =>
    client.get<PageResponse<Invoice>>('/me/account/invoices', { params }).then((r) => r.data),
  payments: (params: { page?: number; size?: number } = {}) =>
    client.get<PageResponse<Payment>>('/me/account/payments', { params }).then((r) => r.data),
  payOutstanding: (amount: number, invoiceId?: number) =>
    client
      .post<{ paymentId: number; providerReference: string; redirectUrl: string }>('/me/payments/initiate', {
        amount,
        invoiceId,
      })
      .then((r) => r.data),
  vehicles: (params: { page?: number; size?: number } = {}) =>
    client.get<PageResponse<Vehicle>>('/me/vehicles', { params }).then((r) => r.data),
  registerVehicle: (body: Omit<Vehicle, 'id' | 'status' | 'residentId' | 'residentName'>) =>
    client.post<Vehicle>('/me/vehicles', body).then((r) => r.data),
  vehicleAccessPass: (id: number) =>
    client.get<{ qrToken: string }>(`/me/vehicles/${id}/access-pass`).then((r) => r.data.qrToken),
  accessPass: () => client.get<{ qrToken: string }>('/me/access-pass').then((r) => r.data.qrToken),
  updateProfile: (body: { fullName: string; phone: string; email?: string; emergencyContact?: string }) =>
    client.put<Resident>('/me/profile', body).then((r) => r.data),
  updateProperty: (body: { houseNumber: string; address?: string }) =>
    client.put<Property>('/me/property', body).then((r) => r.data),
}

export const visitorsApi = {
  mine: (params: { page?: number; size?: number } = {}) =>
    client.get<PageResponse<Visitor>>('/visitors/mine', { params }).then((r) => r.data),
  listAll: (params: { q?: string; page?: number; size?: number } = {}) =>
    client.get<PageResponse<Visitor>>('/visitors', { params }).then((r) => r.data),
  get: (id: number) => client.get<Visitor>(`/visitors/${id}`).then((r) => r.data),
  create: (body: { name: string; phone: string; vehiclePlate?: string; validFrom: string; validUntil: string }) =>
    client.post<Visitor>('/visitors', body).then((r) => r.data),
  cancel: (id: number) => client.put<Visitor>(`/visitors/${id}/cancel`).then((r) => r.data),
  lookup: (qrToken: string) => client.get<VisitorLookup>(`/visitors/lookup/${qrToken}`).then((r) => r.data),
  checkIn: (qrToken: string, gateId?: number) =>
    client.post<Visitor>(`/visitors/checkin/${qrToken}`, null, { params: { gateId } }).then((r) => r.data),
  checkOut: (qrToken: string, gateId?: number) =>
    client.post<Visitor>(`/visitors/checkout/${qrToken}`, null, { params: { gateId } }).then((r) => r.data),
}

export const workersApi = {
  mine: (params: { page?: number; size?: number } = {}) =>
    client.get<PageResponse<Worker>>('/workers/mine', { params }).then((r) => r.data),
  listAll: (params: { q?: string; activeOnly?: boolean; page?: number; size?: number } = {}) =>
    client.get<PageResponse<Worker>>('/workers', { params }).then((r) => r.data),
  listBySponsor: (sponsorResidentId: number, params: { page?: number; size?: number } = {}) =>
    client.get<PageResponse<Worker>>('/workers', { params: { ...params, sponsorResidentId } }).then((r) => r.data),
  get: (id: number) => client.get<Worker>(`/workers/${id}`).then((r) => r.data),
  request: (body: {
    fullName: string
    phone: string
    nationalId?: string
    contractorName: string
    workType: string
    startDate: string
    expectedEndDate: string
    photo?: string
  }) => client.post<Worker>('/workers', body).then((r) => r.data),
  approve: (id: number) => client.post<Worker>(`/workers/${id}/approve`).then((r) => r.data),
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

export const complaintsApi = {
  mine: (params: { page?: number; size?: number } = {}) =>
    client.get<PageResponse<Complaint>>('/complaints/mine', { params }).then((r) => r.data),
  listAll: (params: { status?: ComplaintStatus; page?: number; size?: number } = {}) =>
    client.get<PageResponse<Complaint>>('/complaints', { params }).then((r) => r.data),
  listByResident: (residentId: number, params: { page?: number; size?: number } = {}) =>
    client.get<PageResponse<Complaint>>('/complaints', { params: { ...params, residentId } }).then((r) => r.data),
  create: (body: { category: string; description: string; priority?: string }) =>
    client.post<Complaint>('/complaints', body).then((r) => r.data),
  updateStatus: (id: number, status: ComplaintStatus, assignedTo?: string) =>
    client.put<Complaint>(`/complaints/${id}/status`, { status, assignedTo }).then((r) => r.data),
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
  issue: (body: { tagId: string; assignedResidentId?: number; assignedWorkerId?: number; vehicleId?: number }) =>
    client.post<RfidTag>('/rfid', body).then((r) => r.data),
  revoke: (id: number) => client.put<RfidTag>(`/rfid/${id}/revoke`).then((r) => r.data),
  markLost: (id: number) => client.put<RfidTag>(`/rfid/${id}/lost`).then((r) => r.data),
}

export const securityDashboardApi = {
  get: () => client.get<SecurityDashboard>('/security/dashboard').then((r) => r.data),
}
