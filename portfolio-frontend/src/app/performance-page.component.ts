import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { App } from './app';

@Component({
  selector: 'app-performance-page',
  imports: [CommonModule, FormsModule],
  templateUrl: './performance-page.component.html',
  styleUrl: './app.scss',
})
export class PerformancePageComponent extends App {}
