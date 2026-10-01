import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { App } from './app';

@Component({
  selector: 'app-watchlist-page',
  imports: [CommonModule, FormsModule],
  templateUrl: './watchlist-page.component.html',
  styleUrl: './app.scss',
})
export class WatchlistPageComponent extends App {}
