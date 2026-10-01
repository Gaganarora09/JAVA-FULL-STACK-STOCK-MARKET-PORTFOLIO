import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { App } from './app';

@Component({
  selector: 'app-watchlist-page',
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './watchlist-page.component.html',
  styleUrl: './app.scss',
})
export class WatchlistPageComponent extends App {}
