import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { App } from './app';
import { PortfolioApi } from './portfolio-api.service';

describe('App', () => {
  beforeEach(async () => {
    sessionStorage.removeItem('portfoliopro.jwt');
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [{ provide: PortfolioApi, useValue: { stocks: () => of([]) } }],
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
});
