import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface AuthResponse {
  token: string;
  tokenType: string;
  expiresIn: number;
}

export interface Stock {
  ticker: string;
  companyName: string;
  simulatedPrice: number;
  priceType: string;
  sector: string;
  priceUpdatedAt: string;
}

export interface StockFundamentals {
  ticker: string;
  companyName: string;
  simulatedPrice: number;
  earningsPerShare: number | null;
  priceToEarnings: number | null;
  marketCapitalization: number | null;
  dividendYield: number | null;
  dataSource: string;
  updatedAt: string;
}

export interface StockTechnicalAnalysis {
  ticker: string;
  periodDays: number;
  latestClose: number;
  sma20: number | null;
  rsi14: number | null;
  dataSource: string;
  history: { date: string; close: number }[];
}

export interface Holding {
  ticker: string;
  companyName: string;
  quantity: number;
  averageCostBasis: number;
  currentPrice: number;
  marketValue: number;
  unrealizedGainLoss: number;
}

export interface PortfolioSummary {
  cashBalance: number;
  totalMarketValue: number;
  totalAccountValue: number;
  totalUnrealizedGainLoss: number;
  holdings: Holding[];
}

export interface PortfolioAnalytics {
  totalAccountValue: number;
  cashBalance: number;
  investmentMarketValue: number;
  unrealizedGainLoss: number;
  realizedGainLoss: number;
  totalGainLoss: number;
  cashAllocationPercent: number;
  stockAllocationPercent: number;
  topHoldingConcentrationPercent: number;
  concentrationRiskEstimate: string;
  dailyVolatilityPercent: number | null;
  maxDrawdownPercent: number | null;
  riskMetricsStatus: string;
  riskDescription: string;
  holdings: { ticker: string; companyName: string; sector: string; marketValue: number; allocationPercent: number }[];
  sectors: { sector: string; marketValue: number; allocationPercent: number }[];
}

export interface UserProfile {
  username: string;
  email: string;
  cashBalance: number;
}

export interface PortfolioPerformancePoint {
  date: string;
  cashBalance: number;
  investedValue: number;
  totalValue: number;
  source: string;
}

export interface Trade {
  id: number;
  ticker: string;
  companyName: string;
  type: 'BUY' | 'SELL';
  quantity: number;
  priceAtExecution: number;
  realizedGainLoss: number;
  executedAt: string;
}

export interface WatchlistItem {
  ticker: string;
  companyName: string;
  sector: string;
  simulatedPrice: number;
  priceUpdatedAt: string;
  addedAt: string;
}

@Injectable({ providedIn: 'root' })
export class PortfolioApi {
  private readonly http = inject(HttpClient);

  register(username: string, email: string, password: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>('/api/auth/register', { username, email, password });
  }

  login(username: string, password: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>('/api/auth/login', { username, password });
  }

  stocks(): Observable<Stock[]> {
    return this.http.get<Stock[]>('/api/stocks');
  }

  searchStocks(query: string, sector?: string): Observable<Stock[]> {
    const params: Record<string, string> = {};
    if (query.trim()) params['q'] = query.trim();
    if (sector?.trim()) params['sector'] = sector.trim();
    return this.http.get<Stock[]>('/api/stocks/search', { params });
  }

  fundamentals(ticker: string): Observable<StockFundamentals> {
    return this.http.get<StockFundamentals>(`/api/stocks/${encodeURIComponent(ticker)}/fundamentals`);
  }

  technicalAnalysis(ticker: string, days = 30): Observable<StockTechnicalAnalysis> {
    return this.http.get<StockTechnicalAnalysis>(
      `/api/stocks/${encodeURIComponent(ticker)}/technical-analysis?days=${days}`,
    );
  }

  profile(): Observable<UserProfile> {
    return this.http.get<UserProfile>('/api/users/me');
  }

  summary(): Observable<PortfolioSummary> {
    return this.http.get<PortfolioSummary>('/api/portfolio/me');
  }

  analytics(): Observable<PortfolioAnalytics> {
    return this.http.get<PortfolioAnalytics>('/api/portfolio/analytics');
  }

  performance(range: 'week' | 'month' | 'year' | 'all' = 'month'): Observable<PortfolioPerformancePoint[]> {
    return this.http.get<PortfolioPerformancePoint[]>('/api/portfolio/performance', { params: { range } });
  }

  trades(): Observable<Trade[]> {
    return this.http.get<Trade[]>('/api/trades');
  }

  watchlist(): Observable<WatchlistItem[]> {
    return this.http.get<WatchlistItem[]>('/api/watchlist');
  }

  addWatchlist(ticker: string): Observable<WatchlistItem> {
    return this.http.post<WatchlistItem>(`/api/watchlist/${encodeURIComponent(ticker)}`, {});
  }

  removeWatchlist(ticker: string): Observable<void> {
    return this.http.delete<void>(`/api/watchlist/${encodeURIComponent(ticker)}`);
  }

  trade(ticker: string, type: 'BUY' | 'SELL', quantity: number): Observable<Trade> {
    return this.http.post<Trade>('/api/trades', { ticker, type, quantity });
  }
}
