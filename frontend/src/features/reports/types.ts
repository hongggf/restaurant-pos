export interface TopSellerItem {
  itemName: string;
  count: number;
}

export interface OverviewStatsResponse {
  todayRevenue: number;
  activeTables: number;
  openTickets: number;
  monthlyRevenue: number;
  monthlyVoids: number;
  topSellers: TopSellerItem[];
  activePromoTitle: string | null;
}
