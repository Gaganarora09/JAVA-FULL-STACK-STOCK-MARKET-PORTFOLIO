import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Router } from '@angular/router';
import { Observable, of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { App } from './app';
import { routes } from './app.routes';
import { PortfolioApi, Trade, WatchlistItem } from './portfolio-api.service';

describe('App', () => {
  const apiStub = {
    stocks: () => of([]),
    fundamentals: () => of(null),
    technicalAnalysis: () => of(null),
    addWatchlist: () => of({}),
    watchlist: (): Observable<WatchlistItem[]> => of([]),
    removeWatchlist: () => of(void 0),
  };

  beforeEach(async () => {
    sessionStorage.removeItem('portfoliopro.jwt');
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideRouter(routes), { provide: PortfolioApi, useValue: apiStub }],
    }).compileComponents();
  });

  it('renders the sign-in screen without an existing session', async () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    await fixture.whenStable();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.auth-form-wrap h2')?.textContent).toContain('Sign in');
  });

  it('switches to account creation fields', async () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    fixture.componentInstance.toggleAuthMode();
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.auth-form-wrap h2')?.textContent).toContain('Create your account');
    expect(compiled.querySelector('#email')).not.toBeNull();
  });

  it('normalizes stock selections before loading and navigating', () => {
    const fixture = TestBed.createComponent(App);
    const component = fixture.componentInstance;
    const router = TestBed.inject(Router);
    const navigate = vi.spyOn(router, 'navigate').mockResolvedValue(true);

    component.selectStock(' aapl ');

    expect(component.ticker).toBe('AAPL');
    expect(navigate).toHaveBeenCalledWith(['/stock', 'AAPL']);
  });

  it('labels the active route in the dashboard breadcrumb', async () => {
    const fixture = TestBed.createComponent(App);
    const router = TestBed.inject(Router);

    await router.navigateByUrl('/performance');

    expect(fixture.componentInstance.currentSection).toBe('Performance');
  });

  it('clears private stock analysis when logging out', () => {
    const fixture = TestBed.createComponent(App);
    const component = fixture.componentInstance;

    component.fundamentals.set({
      ticker: 'AAPL',
      companyName: 'Apple Inc.',
      simulatedPrice: 180,
      earningsPerShare: 6,
      priceToEarnings: 30,
      marketCapitalization: 2_000_000_000,
      dividendYield: 0.005,
      dataSource: 'demo',
      updatedAt: '2026-10-01T00:00:00Z',
    });
    component.technicalAnalysis.set({
      ticker: 'AAPL',
      periodDays: 30,
      latestClose: 180,
      sma20: 178,
      rsi14: 52,
      dataSource: 'demo',
      history: [],
    });

    component.logout();

    expect(component.fundamentals()).toBeNull();
    expect(component.technicalAnalysis()).toBeNull();
    expect(component.notice()).toBe('');
    expect(component.error()).toBe('');
  });

  it('filters and paginates persisted trade activity', () => {
    const fixture = TestBed.createComponent(App);
    const component = fixture.componentInstance;
    const trades: Trade[] = Array.from({ length: 7 }, (_, index) => ({
      id: index + 1,
      ticker: 'AAPL',
      companyName: 'Apple Inc.',
      type: index < 6 ? 'BUY' : 'SELL',
      quantity: 1,
      priceAtExecution: 100 + index,
      realizedGainLoss: index === 6 ? 5 : 0,
      executedAt: `2026-10-${String(index + 1).padStart(2, '0')}T00:00:00Z`,
    }));
    component.trades.set(trades);

    component.setActivityFilter('BUY');

    expect(component.filteredTrades).toHaveLength(6);
    expect(component.activityTotalPages).toBe(1);
    expect(component.filteredTrades.every((trade) => trade.type === 'BUY')).toBe(true);

    component.setActivityFilter('ALL');
    component.changeActivityPage(1);

    expect(component.activityTotalPages).toBe(2);
    expect(component.filteredTrades).toHaveLength(1);
    expect(component.filteredTrades[0].id).toBe(7);
  });

  it('resets activity pagination when the selected filter changes', () => {
    const fixture = TestBed.createComponent(App);
    const component = fixture.componentInstance;
    component.trades.set(Array.from({ length: 13 }, (_, index) => ({
      id: index + 1,
      ticker: 'AAPL',
      companyName: 'Apple Inc.',
      type: index < 12 ? 'BUY' : 'SELL',
      quantity: 1,
      priceAtExecution: 100,
      realizedGainLoss: 0,
      executedAt: '2026-10-01T00:00:00Z',
    })));
    component.changeActivityPage(2);

    component.setActivityFilter('SELL');

    expect(component.activityPage).toBe(1);
    expect(component.filteredTrades).toHaveLength(1);
    expect(component.filteredTrades[0].type).toBe('SELL');
  });

  it('derives trade readiness from persisted cash and holdings', () => {
    const fixture = TestBed.createComponent(App);
    const component = fixture.componentInstance;

    component.ticker = 'AAPL';
    component.quantity = 2;
    component.stocks.set([{
      ticker: 'AAPL',
      companyName: 'Apple Inc.',
      currentPrice: 100,
      priceType: 'SIMULATED_DEMO',
      sector: 'Technology',
      primaryExchange: null,
      priceUpdatedAt: '2026-10-01T00:00:00Z',
    }]);
    component.summary.set({
      cashBalance: 250,
      totalMarketValue: 100,
      totalAccountValue: 350,
      totalUnrealizedGainLoss: 0,
      holdings: [{
        ticker: 'AAPL',
        companyName: 'Apple Inc.',
        quantity: 3,
        averageCostBasis: 90,
        currentPrice: 100,
        marketValue: 300,
        unrealizedGainLoss: 30,
      }],
    });

    expect(component.estimatedOrderValue).toBe(200);
    expect(component.canBuy).toBe(true);
    expect(component.canSell).toBe(true);

    component.quantity = 4;

    expect(component.canBuy).toBe(false);
    expect(component.canSell).toBe(false);
  });

  it('refreshes watchlist state after add and remove API calls', () => {
    const fixture = TestBed.createComponent(App);
    const component = fixture.componentInstance;
    const stock = {
      ticker: 'aapl',
      companyName: 'Apple Inc.',
      currentPrice: 100,
      priceType: 'SIMULATED_DEMO',
      sector: 'Technology',
      priceUpdatedAt: null,
      inCatalogue: true,
      primaryExchange: null,
    };
    const watchlistResponse = [{
      ticker: 'AAPL',
      companyName: 'Apple Inc.',
      sector: 'Technology',
      currentPrice: 100,
      priceUpdatedAt: '2026-10-01T00:00:00Z',
      addedAt: '2026-10-01T00:00:00Z',
    }];
    vi.spyOn(apiStub, 'watchlist').mockReturnValue(of(watchlistResponse));
    component.addToWatchlist(stock);

    expect(component.watchlist()).toEqual(watchlistResponse);
    expect(component.notice()).toContain('aapl added');

    component.removeFromWatchlist(watchlistResponse[0]);

    expect(component.watchlist()).toEqual([]);
  });

  it('surfaces watchlist refresh failures after an add', () => {
    const fixture = TestBed.createComponent(App);
    const component = fixture.componentInstance;
    vi.spyOn(apiStub, 'watchlist').mockReturnValue(
      throwError(() => ({ status: 503, error: { message: 'Watchlist unavailable' } })),
    );

    component.addToWatchlist({
      ticker: 'AAPL',
      companyName: 'Apple Inc.',
      currentPrice: 100,
      priceType: 'SIMULATED_DEMO',
      sector: 'Technology',
      priceUpdatedAt: null,
      inCatalogue: true,
      primaryExchange: null,
    });

    expect(component.error()).toBe('Watchlist unavailable');
  });
});
