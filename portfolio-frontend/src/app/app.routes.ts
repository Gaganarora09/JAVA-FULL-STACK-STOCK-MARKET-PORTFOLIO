import { Routes } from '@angular/router';
import { ActivityPageComponent } from './activity-page.component';
import { HoldingsPageComponent } from './holdings-page.component';
import { OverviewPageComponent } from './overview-page.component';
import { PerformancePageComponent } from './performance-page.component';
import { StockDetailPageComponent } from './stock-detail-page.component';
import { WatchlistPageComponent } from './watchlist-page.component';

export const routes: Routes = [
  { path: '', redirectTo: 'overview', pathMatch: 'full' },
  { path: 'overview', component: OverviewPageComponent },
  { path: 'holdings', component: HoldingsPageComponent },
  { path: 'watchlist', component: WatchlistPageComponent },
  { path: 'activity', component: ActivityPageComponent },
  { path: 'performance', component: PerformancePageComponent },
  { path: 'stock/:ticker', component: StockDetailPageComponent },
  { path: '**', redirectTo: 'overview' },
];
