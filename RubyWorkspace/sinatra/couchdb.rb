#!/usr/bin/env ruby
require 'rubygems'
require 'sinatra'
require 'rest_client'
require 'json'

DB = 'http://localhost:5984/eee-meals'

get '/meals/:permalink' do
  data = RestClient.get "#{DB}/#{params[:permalink]}"
  result = JSON.parse(data)
  %Q{
<h1>#{result['title']}</h1>
<p>We enjoyed this meal on #{result['date']}</p>
<p>#{result['summary']}</p>
<p>navigation and links to recipes would go here...</p>
<div>
#{result['description']}
</div>
}
end
