import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Router } from '@angular/router';
import { of } from 'rxjs';
import { vi } from 'vitest';
import { App } from './app';
import { routes } from './app.routes';
import { PortfolioApi } from './portfolio-api.service';

describe('App', () => {
  const apiStub = {
    stocks: () => of([]),
    fundamentals: () => of(null),
    technicalAnalysis: () => of(null),
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
});
