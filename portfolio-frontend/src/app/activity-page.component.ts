import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { App } from './app';

@Component({
  selector: 'app-activity-page',
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './activity-page.component.html',
  styleUrl: './app.scss',
})
export class ActivityPageComponent extends App {}
